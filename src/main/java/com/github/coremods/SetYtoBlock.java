package com.github.coremods;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Random;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Joiner;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.MultimapBuilder.MultimapBuilderWithKeys;
import com.google.common.collect.Multimaps;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.Tuple;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.common.config.Configuration;
import cpw.mods.fml.common.IWorldGenerator;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;

@Mod(modid = SetYtoBlock.MOD_ID, name = SetYtoBlock.MOD_NAME, version = SetYtoBlock.MOD_VERSION)
public class SetYtoBlock {

  public static final String MOD_ID = "SetYtoBlock";
  public static final String MOD_NAME = "Set Y to Block";
  public static final String MOD_VERSION = "0.1.1";

  private static final Logger logger = LogManager.getLogger(SetYtoBlock.class);

  private static final int DEFAULT_MOD_GENERATION_WEIGHT = Integer.MAX_VALUE;
  private static final String[] DEFAULT_DIM_CONFIGS = new String[] {"0 0 lava", "-1 127 lava"};

  @Instance(value = MOD_NAME)
  public static SetYtoBlock instance;

  private final class IWorldGeneratorImplementation implements IWorldGenerator {
    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world,
        IChunkProvider chunkGenerator, IChunkProvider chunkProvider) {

      final Collection<YLevelConfig> yLevelConfigs = dimConfigMap.get(world.provider.dimensionId);

      if (yLevelConfigs != null && yLevelConfigs.size() > 0) {
        final Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);

        ExtendedBlockStorage extendedblockstorage;
        for (YLevelConfig yLevelConfig : yLevelConfigs) {
          final int yShr4 = yLevelConfig.level >> 4;
          extendedblockstorage = chunk.getBlockStorageArray()[yShr4];

          if (extendedblockstorage == null) {
            extendedblockstorage =
                new ExtendedBlockStorage(yLevelConfig.level, !world.provider.hasNoSky);
            chunk.getBlockStorageArray()[yShr4] = extendedblockstorage;
          }

          for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
              extendedblockstorage.func_150818_a(x, yLevelConfig.level & 15, z, yLevelConfig.block);
            }
          }
        }
      }

    }
  }

  private static class YLevelConfig {
    public int level;
    public Block block;

    public YLevelConfig(int level, Block block) {
      this.level = level;
      this.block = block;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((block == null) ? 0 : block.getUnlocalizedName().hashCode());
      result = prime * result + level;
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj != null && obj instanceof YLevelConfig) {
        YLevelConfig other = (YLevelConfig) obj;
        return obj.hashCode() == other.hashCode();
      }
      return false;
    }

    @Override
    public String toString() {
      return "YLevelConfig [level=" + level + ", block=" + block.getUnlocalizedName() + "]";
    }

  }

  @EventHandler
  public void load(FMLInitializationEvent event) {
    GameRegistry.registerWorldGenerator(new IWorldGeneratorImplementation(), modGenerationWeight);
  }

  private Multimap<Integer, YLevelConfig> dimConfigMap;

  private int modGenerationWeight;

  @EventHandler
  public void preInit(FMLPreInitializationEvent event) {
    Configuration config = new Configuration(event.getSuggestedConfigurationFile());

    config.load();

    modGenerationWeight =
        config.get(MOD_NAME, "modGenerationWeight", DEFAULT_MOD_GENERATION_WEIGHT,
            "Mod generation weight (advanced)").getInt();

    String[] dimConfigStrings =
        config
            .get(MOD_NAME, "dimensionParameters", DEFAULT_DIM_CONFIGS,
                "Space delimited list of dimension configurations <dimension> <ylevel> <block type> (eg. 0 1 lava)")
            .getStringList();

    dimConfigMap = MultimapBuilder.hashKeys(256).hashSetValues(256).build();

    for (String confString : dimConfigStrings) {
      try {
        final String[] tokens = confString.trim().split("\\s+");

        Integer dimension = Integer.parseInt(tokens[0]);
        int yLevel = Integer.parseInt(tokens[1]);
        Block toBlock = Block.getBlockFromName(tokens[2]);

        if (toBlock != null) {
          dimConfigMap.put(dimension, new YLevelConfig(yLevel, toBlock));
        } else {
          logger.warn("Unknown block type: " + tokens[2]);
        }

      } catch (NumberFormatException e) {
        logger.warn("Error in config, line: " + confString);
      }
    }

    logger.info("Final Dimension Config Map: "
        + Joiner.on(',').withKeyValueSeparator("=").join(dimConfigMap.asMap()));

    config.save();
  }
}
