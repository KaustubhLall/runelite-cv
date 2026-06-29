/*
 * Copyright (c) 2026
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.cache.item;

import java.awt.image.BufferedImage;
import java.io.IOException;
import net.runelite.cache.definitions.ModelDefinition;
import net.runelite.cache.definitions.ObjectDefinition;
import net.runelite.cache.definitions.providers.ModelProvider;
import net.runelite.cache.definitions.providers.SpriteProvider;
import net.runelite.cache.definitions.providers.TextureProvider;
import net.runelite.cache.models.JagexColor;

public class ObjectSpriteFactory
{
	public static BufferedImage createSprite(ModelProvider modelProvider,
		SpriteProvider spriteProvider, TextureProvider textureProvider,
		ObjectDefinition object) throws IOException
	{
		SpritePixels spritePixels = createSpritePixels(modelProvider, spriteProvider, textureProvider, object);
		return spritePixels == null ? null : spritePixels.toBufferedImage();
	}

	private static SpritePixels createSpritePixels(ModelProvider modelProvider,
		SpriteProvider spriteProvider, TextureProvider textureProvider,
		ObjectDefinition object) throws IOException
	{
		if (object.getObjectModels() == null || object.getObjectModels().length == 0)
		{
			return null;
		}

		// Load the first model for this object (simplified approach)
		int modelId = object.getObjectModels()[0];
		ModelDefinition modelDef = modelProvider.provide(modelId);
		if (modelDef == null)
		{
			return null;
		}

		Model model = getModel(modelProvider, modelDef, object);
		if (model == null)
		{
			return null;
		}

		RSTextureProvider rsTextureProvider = new RSTextureProvider(textureProvider, spriteProvider);
		rsTextureProvider.brightness = JagexColor.BRIGHTNESS_MAX;

		SpritePixels spritePixels = new SpritePixels(36, 32);
		Graphics3D graphics = new Graphics3D(rsTextureProvider);
		graphics.setBrightness(JagexColor.BRIGHTNESS_MAX);
		graphics.setRasterBuffer(spritePixels.pixels, 36, 32);
		graphics.reset();
		graphics.setRasterClipping();
		graphics.setOffset(18, 16);
		graphics.rasterGouraudLowRes = false;

		// Render model
		model.calculateBoundsCylinder();

		// Compute camera distance so the model fits in the sprite
		// The projection uses yOffset as the camera distance for the default orientation=512
		int zoom = Math.max(model.diameter, 32) * 4;
		int zoomVertical = zoom * Graphics3D.SINE[512] >> 16; // = zoom since sin(512)=1

		// Default object rendering parameters - isometric view from front-top
		int rotationX = 512;  // pitch (45 degrees down)
		int rotationY = 1024; // yaw (show front face)
		int rotationZ = 0;

		model.projectAndDraw(graphics, 0,
			rotationY,
			rotationZ,
			rotationX,
			0,
			model.modelHeight / 2 + zoomVertical,
			0);

		graphics.setRasterBuffer(graphics.graphicsPixels,
			graphics.graphicsPixelsWidth,
			graphics.graphicsPixelsHeight);
		graphics.setRasterClipping();
		graphics.rasterGouraudLowRes = true;
		return spritePixels;
	}

	private static Model getModel(ModelProvider modelProvider, ModelDefinition modelDef, ObjectDefinition object) throws IOException
	{
		// Apply object-specific sizing
		if (object.getModelSizeX() != 128 || object.getModelSizeHeight() != 128 || object.getModelSizeY() != 128)
		{
			modelDef.resize(object.getModelSizeX(), object.getModelSizeHeight(), object.getModelSizeY());
		}

		// Apply recoloring
		if (object.getRecolorToFind() != null && object.getRecolorToReplace() != null)
		{
			for (int i = 0; i < object.getRecolorToFind().length && i < object.getRecolorToReplace().length; ++i)
			{
				modelDef.recolor(object.getRecolorToFind()[i], object.getRecolorToReplace()[i]);
			}
		}

		// Apply retexturing
		if (object.getRetextureToFind() != null && object.getTextureToReplace() != null)
		{
			for (int i = 0; i < object.getRetextureToFind().length && i < object.getTextureToReplace().length; ++i)
			{
				modelDef.retexture(object.getRetextureToFind()[i], object.getTextureToReplace()[i]);
			}
		}

		// Apply lighting
		Model model = light(modelDef, object.getAmbient() + 64, object.getContrast() + 768, -50, -10, -50);
		model.calculateBoundsCylinder();
		System.out.println("Object " + object.getId() + " model bounds: diameter=" + model.diameter + ", height=" + model.modelHeight + ", bottomY=" + model.bottomY + ", XYZMag=" + model.XYZMag);
		return model;
	}

	private static Model light(ModelDefinition def, int ambient, int contrast, int x, int y, int z)
	{
		def.computeNormals();
		int somethingMagnitude = (int) Math.sqrt((double) (z * z + x * x + y * y));
		int var7 = somethingMagnitude * contrast >> 8;
		Model litModel = new Model();
		litModel.faceColors1 = new int[def.faceCount];
		litModel.faceColors2 = new int[def.faceCount];
		litModel.faceColors3 = new int[def.faceCount];
		if (def.numTextureFaces > 0 && def.textureCoords != null)
		{
			int[] var9 = new int[def.numTextureFaces];

			int var10;
			for (var10 = 0; var10 < def.faceCount; ++var10)
			{
				if (def.textureCoords[var10] != -1)
				{
					++var9[def.textureCoords[var10] & 255];
				}
			}

			litModel.numTextureFaces = 0;

			for (var10 = 0; var10 < def.numTextureFaces; ++var10)
			{
				if (var9[var10] > 0 && def.textureRenderTypes[var10] == 0)
				{
					++litModel.numTextureFaces;
				}
			}

			litModel.texIndices1 = new int[litModel.numTextureFaces];
			litModel.texIndices2 = new int[litModel.numTextureFaces];
			litModel.texIndices3 = new int[litModel.numTextureFaces];
			var10 = 0;


			for (int i = 0; i < def.numTextureFaces; ++i)
			{
				if (var9[i] > 0 && def.textureRenderTypes[i] == 0)
				{
					litModel.texIndices1[var10] = def.texIndices1[i] & '\uffff';
					litModel.texIndices2[var10] = def.texIndices2[i] & '\uffff';
					litModel.texIndices3[var10] = def.texIndices3[i] & '\uffff';
					var9[i] = var10++;
				}
				else
				{
					var9[i] = -1;
				}
			}

			litModel.textureCoords = new byte[def.faceCount];

			for (int i = 0; i < def.faceCount; ++i)
			{
				if (def.textureCoords[i] != -1)
				{
					litModel.textureCoords[i] = (byte) var9[def.textureCoords[i] & 255];
				}
				else
				{
					litModel.textureCoords[i] = -1;
				}
			}
		}

		for (int faceIdx = 0; faceIdx < def.faceCount; ++faceIdx)
		{
			byte faceType;
			if (def.faceRenderTypes == null)
			{
				faceType = 0;
			}
			else
			{
				faceType = def.faceRenderTypes[faceIdx];
			}

			byte faceAlpha;
			if (def.faceTransparencies == null)
			{
				faceAlpha = 0;
			}
			else
			{
				faceAlpha = def.faceTransparencies[faceIdx];
			}

			short faceTexture;
			if (def.faceTextures == null)
			{
				faceTexture = -1;
			}
			else
			{
				faceTexture = def.faceTextures[faceIdx];
			}

			if (faceAlpha == -2)
			{
				faceType = 3;
			}

			if (faceAlpha == -1)
			{
				faceType = 2;
			}

			net.runelite.cache.models.VertexNormal vertexNormal;
			int tmp;
			net.runelite.cache.models.FaceNormal faceNormal;
			if (faceTexture == -1)
			{
				if (faceType != 0)
				{
					if (faceType == 1)
					{
						faceNormal = def.faceNormals[faceIdx];
						tmp = (y * faceNormal.y + z * faceNormal.z + x * faceNormal.x) / (var7 / 2 + var7) + ambient;
						litModel.faceColors1[faceIdx] = method2608(def.faceColors[faceIdx] & '\uffff', tmp);
						litModel.faceColors3[faceIdx] = -1;
					}
					else if (faceType == 3)
					{
						litModel.faceColors1[faceIdx] = 128;
						litModel.faceColors3[faceIdx] = -1;
					}
					else
					{
						litModel.faceColors3[faceIdx] = -2;
					}
				}
				else
				{
					int var15 = def.faceColors[faceIdx] & '\uffff';
					vertexNormal = def.vertexNormals[def.faceIndices1[faceIdx]];

					tmp = (y * vertexNormal.y + z * vertexNormal.z + x * vertexNormal.x) / (var7 * vertexNormal.magnitude) + ambient;
					litModel.faceColors1[faceIdx] = method2608(var15, tmp);
					vertexNormal = def.vertexNormals[def.faceIndices2[faceIdx]];

					tmp = (y * vertexNormal.y + z * vertexNormal.z + x * vertexNormal.x) / (var7 * vertexNormal.magnitude) + ambient;
					litModel.faceColors2[faceIdx] = method2608(var15, tmp);
					vertexNormal = def.vertexNormals[def.faceIndices3[faceIdx]];

					tmp = (y * vertexNormal.y + z * vertexNormal.z + x * vertexNormal.x) / (var7 * vertexNormal.magnitude) + ambient;
					litModel.faceColors3[faceIdx] = method2608(var15, tmp);
				}
			}
			else if (faceType != 0)
			{
				if (faceType == 1)
				{
					faceNormal = def.faceNormals[faceIdx];
					tmp = (y * faceNormal.y + z * faceNormal.z + x * faceNormal.x) / (var7 / 2 + var7) + ambient;
					litModel.faceColors1[faceIdx] = bound2to126(tmp);
					litModel.faceColors3[faceIdx] = -1;
				}
				else
				{
					litModel.faceColors3[faceIdx] = -2;
				}
			}
			else
			{
				vertexNormal = def.vertexNormals[def.faceIndices1[faceIdx]];

				tmp = (y * vertexNormal.y + z * vertexNormal.z + x * vertexNormal.x) / (var7 * vertexNormal.magnitude) + ambient;
				litModel.faceColors1[faceIdx] = bound2to126(tmp);
				vertexNormal = def.vertexNormals[def.faceIndices2[faceIdx]];

				tmp = (y * vertexNormal.y + z * vertexNormal.z + x * vertexNormal.x) / (var7 * vertexNormal.magnitude) + ambient;
				litModel.faceColors2[faceIdx] = bound2to126(tmp);
				vertexNormal = def.vertexNormals[def.faceIndices3[faceIdx]];

				tmp = (y * vertexNormal.y + z * vertexNormal.z + x * vertexNormal.x) / (var7 * vertexNormal.magnitude) + ambient;
				litModel.faceColors3[faceIdx] = bound2to126(tmp);
			}
		}

		litModel.verticesCount = def.vertexCount;
		litModel.verticesX = def.vertexX;
		litModel.verticesY = def.vertexY;
		litModel.verticesZ = def.vertexZ;
		litModel.indicesCount = def.faceCount;
		litModel.indices1 = def.faceIndices1;
		litModel.indices2 = def.faceIndices2;
		litModel.indices3 = def.faceIndices3;
		litModel.facePriorities = def.faceRenderPriorities;
		litModel.faceTransparencies = def.faceTransparencies;
		litModel.faceTextures = def.faceTextures;
		return litModel;
	}

	static int method2608(int var0, int var1)
	{
		var1 = ((var0 & 127) * var1) >> 7;
		var1 = bound2to126(var1);

		return (var0 & 65408) + var1;
	}

	static int bound2to126(int var0)
	{
		if (var0 < 2)
		{
			var0 = 2;
		}
		else if (var0 > 126)
		{
			var0 = 126;
		}

		return var0;
	}
}
