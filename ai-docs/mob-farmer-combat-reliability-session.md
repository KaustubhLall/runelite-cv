# Mob Farmer combat/loot/login reliability — session record (2026-06-28)

Full record of the multi-part debugging+fix session so nothing is lost. Covers what was
tried, what worked, what didn't, *why*, learnings, future design, and how to extend the
ideas to the other farmers (mining/woodcutting/skill) and future automation.

Branch: `codex/mob-farmer-attack-cadence`. Character/port map this session (ports rotate on
relaunch via localPort fallback): **account1 = mining (CoreDump / "C0re Dumped")**,
**account2 = cow ("nullptrexcpt")**, **account3 = cow ("bus err")**.

---

## 1. The original ask (OSR-52 follow-up): attacks getting cancelled by loot/bury

**Problem:** burying bones / picking up loot on the same tick as a swing cancelled the
attack (lost hit + XP). User insight: wait for the XP drop (proof the swing landed), *then*
bury/move on that tick — it's safe.

**Grounded against OSRS wiki:** melee hit delay = 0 → damage+XP land on the exact swing
tick, so a combat XP drop is proof the swing resolved. An interrupting "named-tick" action
(bury) in the per-tick action queue can delete a weaker queued command (the swing) on the
same tick. (Ranged/magic XP lags via projectile distance — melee-only guarantee.)

**What shipped & works:**
- Combat XP tracking in `onStatChanged` (ATTACK/STR/DEF/HP/RANGED/MAGIC), `lastMobFarmerCombatXpDropTick`.
- **Measured attack speed**: mode of recent inter-XP-drop gaps (deduped per tick) →
  `mobFarmerEffectiveAttackInterval()`. Auto-adapts to weapon/style/ranged-rapid/autocast/
  spellbook with no lookup table; static fallback = equipped weapon `ItemStats.getEquipment().getAspeed()`
  then the configured value. Confirmed `meas=4` live on cows.
- **Concurrent in-place bury**: burying in the post-XP safe window does NOT break a melee
  auto-attack (verified live — cadence stays tick-perfect through buries). So bury happens
  *during* combat in that window, not as a separate session that steals swings.
- Batched burying (`mobFarmerMinBuryBatch`, default 4; override when inventory full).
- Pending-attack guard (`mobFarmerAttackPending()`): after an attack is commanded but before
  the XP confirms, the player may not be `interacting` yet — naive "not in combat" checks
  would wrongly allow a loot/bury click that cancels the pending swing. Hold until confirmed
  or timeout.

**What didn't work / was corrected:**
- First patch only gated the two combat-window call sites and treated `interacting==null` as
  safe → a loot/bury right after `menu-attack` still cancelled the swing. Fixed by the
  top-of-loop "attack always wins" gate + pending-attack guard.

---

## 2. "Too loot hungry / skips attacks even with valid targets"

**Root causes (found via live HTTP `combat-idle` monitor):**
1. **Bone-chasing**: `lootOwnershipMode=ANY` + RuneLite Ground Items highlighting OTHER
   players' bones at a busy spot → the bot walked across the field (pathDistance 8-9) looting
   everyone's 29gp bones instead of fighting the cow at d=1. The loot-travel cadence guard
   (`applyMobFarmerLootTravelPolicy`, `normalLootMaxMissedAttacks=0`) only triggered while
   *currently interacting*; between kills it was off, so far loot was allowed.
2. **Far-cow chasing at contested spots**: `engagedMode=FREE_ONLY` skips cows already engaged
   by other players; when all near cows were taken it walked to a far free cow (d=12-15).

**Fixes (config, no rebuild — validated live, combat uptime ~15% → ~65%):**
- `lootOwnershipMode=OWN_ONLY` — only loot your own kills' drops (spawn at your feet → bury
  concurrently, fight everything else). The ownership reject is unconditional; Ground-Items
  highlight does NOT bypass it.
- `engagedMode=PREFER_FREE` — fight the nearest, prefer uncontested, don't walk across the field.

**Open code follow-up — SHIPPED (2026-06-28, see addendum below):** the loot-travel policy now
defers loot that is farther than an in-range attack target, so far loot is rejected between
kills too.

---

## 3. Attack-style selection (new feature)

`mobFarmerAttackStyleSlot` (0=off, 1-4 = Combat Options slot top→bottom) → when the current
`VarPlayer.ATTACK_STYLE` (43) differs, clicks `InterfaceID.CombatInterface._0.._3` (group 593,
children 6/10/14/18) via menuAction. Best-effort: defers if the combat tab interface isn't
loaded (open the combat tab once). Which stat each slot trains is weapon-dependent. Default off.

---

## 4. AFK login/disconnect recovery failing on some clients

**Symptom:** after idle-logout, two clients sat at LOGIN_SCREEN looping `click-queued →
cooldown` without logging in.

**Root cause:** recovery used `new Robot()` OS-level focus-click + global `keyPress(VK_ENTER)`.
OS input only reaches the window with Windows keyboard focus → a backgrounded / secondary-
monitor client never gets the Enter (and a Robot key can leak into whatever IS focused).

**Fix (shipped, focus-independent — account1/account2 logged in through it):** dispatch ENTER
straight to THIS client's canvas on the EDT (`dispatchMobFarmerCanvasKey`) instead of Robot;
`clickLoginScreen` now prefers this canvas-Enter even when the click-to-play widget is visible.
Shared by mob farmer AND skill (mining/WC) farmers via `tryGenericLoginRecovery`, so mining
recovery is fixed too. Note: idle-logout itself takes >20 min, so keeping clients *in combat*
(below) is the real defence — recovery is the backstop.

---

## 5. THE big one: "stuck combat / attacks don't register after AFK"

**Symptom:** engaged or trying to engage, `attack:invoked` fires every tick (scheduler allowed),
target is a valid adjacent reachable cow, but it never engages — `healthRatio=-1`, no XP for
thousands of ticks, player won't walk/attack. The game tick advances (cows wander) so the
*client* is alive; only **action processing** is frozen.

**Hypotheses tried and the verdict on each:**
- ❌ **Mouse off-canvas** (`getMouseCanvasPosition()==(-1,-1)`): plausible and I shipped a
  `primeMobFarmerCanvasMouse()` (synthetic MOUSE_MOVED before each menuAction) — but DISPROVEN
  by the differential: account2 farms fine with mouse `(-1,-1)`, account3 froze with a valid
  `(397,251)`. `setMouseCanvasPosition` doesn't even exist (getter only). Prime kept as harmless.
- ❌ **Window focus**: account2 farms fully unfocided/background (foreground was a third window).
- ❌ **Minimized window**: both visible, not iconic.
- ❌ **Location/pathing**: account3 had 7 cows reachable=True path=2 blockedByDoor=False.
- ❌ **Per-profile RuneLite config/plugin**: account2 vs account3 configs are identical (only
  timestamps differ).
- ✅ **A real ground click resets it** (user-confirmed: "when I click on the ground it resets
  things"). This explains why account2 — which kept *engaging* (auto-attack self-sustains, no
  per-swing menuAction) — never froze, while account3 (stuck trying to land the first engage)
  stayed frozen.

**Fix (shipped):** `tryMobFarmerStuckCombatRecovery` rewritten as a **focus-independent
ground-click guard**: when there is a reachable target nearby but no combat XP for the stall
window (`mobFarmerCombatStallTicks`, 0=auto 3×interval, min 8), dispatch a synthetic left-click
(`mobFarmerCanvasGroundClickReset`) to the client's canvas — on the nearest target's mapped
canvas point if available (attack/engage), else a viewport ground point (walk) — to wake the
input pipeline. Throttled; only fires when it *should* be fighting, so it never disturbs a
client that's actually farming. Called early in the loop (covers the can't-engage case, not
just the engaged case).

**Status:** account2 + mining run great on the final build. account3 ("bus err") still
reverts (the guard fires and briefly engages but it keeps falling back). Since an identical
build/config farms perfectly on account2, account3's residue is account/client-runtime-
specific (not code) and needs on-screen inspection (computer-use, which needs the user awake
to authorise). Leading remaining guess: the character is in a genuinely stuck in-game state or
the client instance is degraded — a fresh relaunch did not clear it.

---

## Key learnings / mental models
- **The XP drop is the ground truth for combat timing.** Measure cadence from it; confirm
  swings with it; never trust tick-count estimates from *click* timestamps (they drift out of
  phase with the engine's auto-attack).
- **Movement breaks combat; in-place actions don't.** Bury/scatter in place are safe during a
  swing (in the post-XP window); anything that walks (loot pickup, retarget) costs the swing.
  Design around: attack-always-wins, loot only in genuine downtime, bury concurrently.
- **OS-level input (Robot) is the wrong tool for multi-client/background automation.** It needs
  OS focus and can leak across windows/monitors. Prefer **synthetic AWT events dispatched to
  `client.getCanvas()`** (KeyEvent/MouseEvent on the EDT) — per-client, focus/monitor-independent.
- **The client can stop honouring invoked menu actions after no real input for a while; a real
  (synthetic-to-canvas) click resets it.** Build a stuck-watchdog around the XP-drop signal.
- **Diagnose live over the HTTP endpoints, not by guessing.** The `combat-idle`/`fastlog`
  monitors (scratchpad PowerShell, sampling `/automation/.../status`) cracked every root cause;
  always capture the differential between a working and a broken client.

## Common-sense instructions to extend to other implemented things
- **Skill farmers (mining/WC) already share `tryGenericLoginRecovery`** → they got the
  canvas-Enter login fix for free. Verify mining recovery the same way (it works as of this
  session).
- **Apply `primeMobFarmerCanvasMouse()` + the canvas-event pattern to every place that uses
  `client.menuAction` / Robot** across mining/WC/action-slots (some sites already primed here).
  Anything that depends on OS focus or a Robot click should move to canvas dispatch.
- **Add a stuck-XP watchdog to the skill farmers too** (mining/WC): "running + target reachable
  + no skill XP for N ticks → ground-click reset". Same shape as the combat watchdog, keyed on
  Mining/Woodcutting XP instead of combat XP.
- **Measured-interval idea generalises**: any cadence (mining rocks, WC trees, alching) can be
  measured from its XP-drop gaps rather than hard-coded.
- **Loot/ownership/engaged defaults**: `OWN_ONLY` + `PREFER_FREE` are the sane defaults at busy
  F2P spots; consider making them the code defaults for the mob farmer.

---

## Addendum (2026-06-28 pm): five cadence fixes + loot stutter-step

Branch `codex/mob-farmer-attack-cadence`. All changes are mob-farmer-scoped (mining/WC untouched).

1. **Bury cancelled a fresh-target attack.** `mobFarmerInBurySafeWindow()` / the
   `confirmed-attack-window` branch trusted a *stale* XP drop from the previous kill, so an
   in-place bury fired while still walking into a new target and cancelled the queued
   walk-to-attack. New `mobFarmerXpDropMatchesCurrentEngagement()` (XP-drop tick ≥ last COMBAT
   command tick) gates both — no bury until a swing lands on the *current* target.
2. **In-range attack beats a loot-walk** (`mobFarmerTargetWithinAttackRange`, make-progress path).
3. **Fast retarget when a mob is stolen** (`mobFarmerTargetStolenByOther`): NPC now interacting
   with another player + no combat XP for > one interval → drop it this tick.
4. **Gate priority over attack**: if the target needs an unsatisfied door, open it *before* the
   combat gate (`mobFarmerTargetHasPendingDoor`), else re-Attack made the client path the long way
   and oscillate N/S along the barrier. Skips on door-timeout so it can't wedge the loop.
5. **Idle/mouse-takeover reset relaxed**: `mobFarmerCombatStallTicks` default 0→100 (~1 min). NOTE:
   a *stored* 0 in a profile overrides the code default (→ auto 3×interval=12). Had to POST
   `combatStallTicks=100` to `/automation/mob-farmer/config` on account3 to actually relax it.

**The big one — "walks away to bones, no stutter-step."** Worked out live: RuneLite Ground Items
highlights own bones → farmer flags them `priority:ground-items-highlighted`, which (a) bypasses
the value floor and (b) is `highPriority`, so `applyMobFarmerLootTravelPolicy`'s cadence-cost gate
(only `!highPriority && combatActive`) never rejects them — and `combatActive` is false between
kills anyway. Result: it walked 3-7 tiles to highlighted bones with a cow at pathDistance 0-1.
**Fix:** in the travel policy, publish the nearest selectable target's path distance
(`mobFarmerNearestAttackTargetPathDistance`, set each step from `earlySelection`) and reject any
loot that is *farther than an in-range mob* (target pd ≤ 1) unless it's on the extreme-value
`combatInterruptItems` list (`cadenceDecision=deferred-attack-in-range`). One chokepoint covers all
loot phases. Underfoot bones (pd 0) still loot/bury normally. Verified live on `bus err`: 0 walk-
away events vs 44 correct deferrals, combat XP still climbing, loot+bury still firing in downtime.
