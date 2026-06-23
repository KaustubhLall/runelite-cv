/*
 * Copyright (c) 2026
 * All rights reserved.
 */
package net.runelite.client.plugins.cvhelper;

import com.google.common.collect.EvictingQueue;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.ScriptID;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.vars.InputType;
import net.runelite.client.callback.ClientThread;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Singleton
@Slf4j
public class ChatResponderService
{
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
	private static final String DEFAULT_MODEL = "gpt-4o-mini";
	private static final int MAX_HISTORY = 60;
	private static final int MAX_RESPONSE_CHARS = 100;
	private static final long MIN_COOLDOWN_MS = 5000L;

	private static final Set<ChatMessageType> RESPONDABLE_TYPES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
		ChatMessageType.PUBLICCHAT,
		ChatMessageType.MODCHAT,
		ChatMessageType.PRIVATECHAT,
		ChatMessageType.FRIENDSCHAT,
		ChatMessageType.CLAN_CHAT,
		ChatMessageType.CLAN_GUEST_CHAT
	)));

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private OkHttpClient okHttpClient;

	@Inject
	private Gson gson;

	@Inject
	private CvHelperConfig config;

	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r ->
	{
		Thread t = new Thread(r, "cvhelper-chat-responder");
		t.setDaemon(true);
		return t;
	});

	private final AtomicBoolean running = new AtomicBoolean(false);
	private final EvictingQueue<ChatLine> history = EvictingQueue.create(MAX_HISTORY);
	private final Set<String> recentSignatures = Collections.synchronizedSet(new HashSet<>());
	private final AtomicReference<String> lastResponse = new AtomicReference<>("");
	private final AtomicReference<String> status = new AtomicReference<>("idle");
	private volatile long lastResponseTime = 0;
	private volatile long lastPollTime = 0;

	public static final class ChatLine
	{
		public final String sender;
		public final String message;
		public final ChatMessageType type;
		public final long time;
		public final boolean fromSelf;

		ChatLine(String sender, String message, ChatMessageType type, long time, boolean fromSelf)
		{
			this.sender = sender;
			this.message = message;
			this.type = type;
			this.time = time;
			this.fromSelf = fromSelf;
		}
	}

	public void start()
	{
		if (running.compareAndSet(false, true))
		{
			scheduler.scheduleAtFixedRate(this::poll, 4, 4, TimeUnit.SECONDS);
			status.set("started");
			log.info("Chat responder started");
		}
	}

	public void stop()
	{
		if (running.compareAndSet(true, false))
		{
			scheduler.shutdownNow();
			history.clear();
			recentSignatures.clear();
			lastResponse.set("");
			status.set("stopped");
			log.info("Chat responder stopped");
		}
	}

	public void onChatMessage(ChatMessage event)
	{
		if (!running.get() || !config.chatResponderEnabled())
		{
			return;
		}

		ChatMessageType type = event.getType();
		if (!RESPONDABLE_TYPES.contains(type))
		{
			return;
		}

		String message = stripTags(event.getMessage());
		if (message == null || message.trim().isEmpty())
		{
			return;
		}

		String sender = event.getName();
		boolean fromSelf = isLocalPlayer(sender);
		if (fromSelf)
		{
			return;
		}

		String signature = signature(type, sender, message);
		if (recentSignatures.contains(signature))
		{
			return;
		}
		recentSignatures.add(signature);

		ChatLine line = new ChatLine(sender, message, type, System.currentTimeMillis(), fromSelf);
		synchronized (history)
		{
			history.add(line);
		}
	}

	public Map<String, Object> getStatus()
	{
		List<Map<String, Object>> recent = new ArrayList<>();
		long now = System.currentTimeMillis();
		long windowMs = windowMs();
		synchronized (history)
		{
			for (ChatLine line : history)
			{
				if (now - line.time <= windowMs)
				{
					Map<String, Object> m = new java.util.LinkedHashMap<>();
					m.put("sender", line.sender);
					m.put("message", line.message);
					m.put("type", line.type.name());
					m.put("time", line.time);
					recent.add(m);
				}
			}
		}
		Map<String, Object> map = new java.util.LinkedHashMap<>();
		map.put("enabled", config.chatResponderEnabled());
		map.put("running", running.get());
		map.put("status", status.get());
		map.put("lastResponse", lastResponse.get());
		map.put("lastResponseTime", lastResponseTime);
		map.put("recentMessages", recent);
		map.put("recentSignatures", recentSignatures.size());
		map.put("minMessages", config.chatResponderMinMessages());
		map.put("windowSeconds", config.chatResponderWindowSeconds());
		map.put("cooldownSeconds", config.chatResponderCooldownSeconds());
		map.put("model", effectiveModel());
		return map;
	}

	private void poll()
	{
		lastPollTime = System.currentTimeMillis();
		if (!config.chatResponderEnabled())
		{
			status.set("disabled");
			return;
		}
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			status.set("waiting: not logged in");
			return;
		}

		long now = System.currentTimeMillis();
		long windowMs = windowMs();
		int minMessages = Math.max(1, config.chatResponderMinMessages());
		long cooldownMs = Math.max(MIN_COOLDOWN_MS, config.chatResponderCooldownSeconds() * 1000L);

		if (now - lastResponseTime < cooldownMs)
		{
			status.set("waiting: cooldown");
			return;
		}

		List<ChatLine> recent;
		synchronized (history)
		{
			recent = history.stream()
				.filter(m -> now - m.time <= windowMs)
				.filter(m -> !m.fromSelf)
				.collect(Collectors.toList());
		}

		long uniqueMessages = recent.stream()
			.map(m -> normalize(m.message))
			.distinct()
			.count();
		long uniqueSenders = recent.stream()
			.map(m -> m.sender)
			.distinct()
			.count();

		if (recent.size() < minMessages || uniqueMessages < Math.max(2, minMessages / 2))
		{
			status.set("waiting: recent=" + recent.size() + " unique=" + uniqueMessages);
			return;
		}

		if (isRepetitive(recent))
		{
			status.set("waiting: repetitive");
			return;
		}

		String context = buildContext(recent);
		status.set("generating");
		callOpenAI(context);
	}

	private void callOpenAI(String context)
	{
		String apiKey = System.getenv("OPENAI_API_KEY");
		if (apiKey == null || apiKey.isEmpty())
		{
			log.warn("Chat responder: OPENAI_API_KEY not set in environment");
			status.set("error: OPENAI_API_KEY not set");
			return;
		}

		String model = effectiveModel();
		JsonObject request = new JsonObject();
		request.addProperty("model", model);
		request.addProperty("max_tokens", 60);
		request.addProperty("temperature", 0.85);
		request.addProperty("top_p", 0.95);

		JsonArray messages = new JsonArray();
		JsonObject system = new JsonObject();
		system.addProperty("role", "system");
		system.addProperty("content", buildPrompt());
		messages.add(system);

		JsonObject user = new JsonObject();
		user.addProperty("role", "user");
		user.addProperty("content", context);
		messages.add(user);

		request.add("messages", messages);

		RequestBody body = RequestBody.create(JSON, gson.toJson(request));
		Request req = new Request.Builder()
			.url(OPENAI_API_URL)
			.header("Authorization", "Bearer " + apiKey)
			.post(body)
			.build();

		okHttpClient.newCall(req).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.warn("Chat responder OpenAI request failed", e);
				status.set("error: " + e.getMessage());
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (response)
				{
					if (!response.isSuccessful())
					{
						String bodyText = response.body() != null ? response.body().string() : "";
						log.warn("Chat responder OpenAI error HTTP {}: {}", response.code(), bodyText);
						status.set("error: HTTP " + response.code());
						return;
					}
					String bodyText = response.body().string();
					JsonObject json = gson.fromJson(bodyText, JsonObject.class);
					JsonArray choices = json.getAsJsonArray("choices");
					if (choices == null || choices.size() == 0)
					{
						status.set("error: no choices");
						return;
					}
					JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
					if (message == null)
					{
						status.set("error: no message");
						return;
					}
					JsonElement contentEl = message.get("content");
					if (contentEl == null || !contentEl.isJsonPrimitive())
					{
						status.set("error: no content");
						return;
					}
					String text = contentEl.getAsString();
					if (text != null && !text.trim().isEmpty())
					{
						String responseText = cleanResponse(text.trim());
						if (!responseText.isEmpty())
						{
							sendResponse(responseText);
						}
						else
						{
							status.set("skipped: empty response");
						}
					}
					else
					{
						status.set("skipped: empty response");
					}
				}
			}
		});
	}

	private void sendResponse(String text)
	{
		lastResponse.set(text);
		lastResponseTime = System.currentTimeMillis();
		status.set("sending: " + text);

		clientThread.invokeLater(() ->
		{
			try
			{
				if (client.getGameState() != GameState.LOGGED_IN)
				{
					status.set("error: not logged in");
					return;
				}
				if (client.getVarcIntValue(VarClientID.MESLAYERMODE) != InputType.NONE.getType())
				{
					status.set("waiting: input active");
					return;
				}
				client.runScript(ScriptID.CHAT_SEND, text, ChatMessageType.PUBLICCHAT.getType(), 0, 0, -1);
				log.info("Chat responder sent: {}", text);
				status.set("responded: " + text);
			}
			catch (Exception e)
			{
				log.warn("Chat responder failed to send", e);
				status.set("error: " + e.getMessage());
			}
		});
	}

	private String buildPrompt()
	{
		String custom = config.chatResponderPrompt();
		if (custom != null && !custom.trim().isEmpty())
		{
			return custom.trim();
		}
		return DEFAULT_PROMPT;
	}

	private String buildContext(List<ChatLine> recent)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Recent chat messages (newest last):\n");
		for (ChatLine line : recent)
		{
			sb.append("[").append(line.type.name()).append("] ");
			if (line.sender != null && !line.sender.isEmpty())
			{
				sb.append(line.sender).append(": ");
			}
			sb.append(line.message).append("\n");
		}
		sb.append("\nReply as a real OSRS player. Keep it short.");
		return sb.toString();
	}

	private String cleanResponse(String text)
	{
		String cleaned = stripTags(text)
			.replaceAll("\\r\\n|\\n", " ")
			.replaceAll("\\s+", " ")
			.trim();
		if (cleaned.toLowerCase(Locale.ROOT).startsWith("no_reply"))
		{
			return "";
		}
		if (cleaned.length() > MAX_RESPONSE_CHARS)
		{
			cleaned = cleaned.substring(0, MAX_RESPONSE_CHARS);
			int lastSpace = cleaned.lastIndexOf(' ');
			if (lastSpace > 20)
			{
				cleaned = cleaned.substring(0, lastSpace);
			}
		}
		return cleaned;
	}

	private boolean isRepetitive(List<ChatLine> lines)
	{
		if (lines.size() < 3)
		{
			return false;
		}
		String first = normalize(lines.get(0).message);
		long same = lines.stream()
			.map(l -> normalize(l.message))
			.filter(m -> m.equals(first))
			.count();
		return same > lines.size() / 2;
	}

	private String normalize(String message)
	{
		return stripTags(message).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
	}

	private String stripTags(String message)
	{
		if (message == null)
		{
			return "";
		}
		return message.replaceAll("<[^>]*>", "");
	}

	private String signature(ChatMessageType type, String sender, String message)
	{
		return type.name() + "|" + sender + "|" + normalize(message);
	}

	private boolean isLocalPlayer(String name)
	{
		if (name == null)
		{
			return false;
		}
		Player local = client.getLocalPlayer();
		String localName = local == null ? null : local.getName();
		return name.equalsIgnoreCase(localName);
	}

	private String effectiveModel()
	{
		String model = config.chatResponderModel();
		if (model == null || model.trim().isEmpty())
		{
			return DEFAULT_MODEL;
		}
		return model.trim();
	}

	private long windowMs()
	{
		return Math.max(5000L, config.chatResponderWindowSeconds() * 1000L);
	}

	private static final String DEFAULT_PROMPT =
		"You are a casual Old School RuneScape player chatting in public. You are not an AI, bot, or assistant. " +
		"Respond naturally and briefly, like a real human player.\n\n" +
		"Rules:\n" +
		"- Keep replies under 2 short sentences, one line if possible.\n" +
		"- Use OSRS slang: ty, np, gl, glhf, rekt, rip, lmao, lol, nope, ye, nah, gg, tyvm, wyd, omw.\n" +
		"- Reference real OSRS things: skills, quests, bosses, items, minigames, GE, lumby, fally, varrock, wildy, barrows, zulrah, vorkath.\n" +
		"- Be friendly, sometimes sarcastic, sometimes helpful. Don't be overly enthusiastic.\n" +
		"- Make occasional small typos or use lowercase to feel human.\n" +
		"- If the chat is weird, off-topic, or spammy, ignore it or reply with a short confused line.\n" +
		"- Never say you are an AI, model, bot, or assistant. Never explain yourself.\n" +
		"- Never mention OpenAI, ChatGPT, training data, or prompts.\n" +
		"- Never answer questions about how you work or what you are.\n" +
		"- If someone asks for money or doubling, be skeptical or joke about it.\n" +
		"- If you don't know something, say so briefly.\n\n" +
		"Classic phrases you might use: buying gf 10k, trimming armor free, fally west bank, need 1 more for giants, " +
		"selling lobs 200ea, doubling money legit (jk), 62,059 left, salmoneus, nice, cannonballs?, ty, np, gl.\n\n" +
		"Examples of how to respond:\n" +
		"- \"anyone selling lobs?\" -> \"ye how many u need\"\n" +
		"- \"where is fally west bank?\" -> \"west of fally castle, near the pub\"\n" +
		"- \"trimming armor?\" -> \"free trims? sounds legit lol\"\n" +
		"- \"buying gf 10k\" -> \"10k? inflation hit hard\"\n" +
		"- \"help with dragon slayer?\" -> \"gl with elvarg, bring antifire\"\n" +
		"- \"nice\" -> \"ty\"\n" +
		"- \"gz\" -> \"ty\"\n" +
		"- \"what u doing?\" -> \"wc by seers, u?\"\n" +
		"- \"doubling money\" -> \"sounds legit, 1 trade no scam?\"\n" +
		"\n" +
		"Now respond to the chat as a normal OSRS player.";
}
