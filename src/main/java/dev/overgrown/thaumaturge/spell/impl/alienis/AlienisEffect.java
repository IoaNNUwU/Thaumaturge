package dev.overgrown.thaumaturge.spell.impl.alienis;

import dev.overgrown.thaumaturge.spell.pattern.AspectEffect;
import dev.overgrown.thaumaturge.spell.tier.AoeSpellDelivery;
import dev.overgrown.thaumaturge.spell.tier.SelfSpellDelivery;
import dev.overgrown.thaumaturge.spell.tier.TargetedSpellDelivery;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.Heightmap;

import java.util.EnumSet;
import java.util.Set;

public class AlienisEffect implements AspectEffect {

    @Override
    public void apply(SelfSpellDelivery delivery) {
        delivery.addEffect(caster -> {
            MinecraftServer server = caster.getServer();
            if (server == null) return;

            ServerWorld targetWorld = server.getOverworld();
            TeleportTarget spawnTarget = caster.getRespawnTarget(false, TeleportTarget.NO_OP);

            if (spawnTarget == null) {
                BlockPos spawnPos = targetWorld.getSpawnPos();
                spawnTarget = new TeleportTarget(
                        targetWorld,
                        Vec3d.ofCenter(spawnPos),
                        Vec3d.ZERO,
                        caster.getYaw(),
                        caster.getPitch(),
                        TeleportTarget.NO_OP
                );
            }

            caster.teleportTo(spawnTarget);
            caster.fallDistance = 0;

            targetWorld.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0F, 1.0F);
            targetWorld.spawnParticles(ParticleTypes.PORTAL,
                    caster.getX(), caster.getY() + 1, caster.getZ(), 20,
                    0.5, 0.5, 0.5, 0.1);
        });
    }

    @Override
    public void apply(TargetedSpellDelivery delivery) {
        delivery.addOnHitEffect(entity -> teleportEntityRandomly(entity, 16.0F));
    }

    @Override
    public void apply(AoeSpellDelivery delivery) {
        delivery.addEffect(pos -> {
            ServerWorld world = delivery.getCasterWorld();
            Box area = new Box(pos).expand(5.0);
            world.getEntitiesByClass(Entity.class, area, e -> true).forEach(entity -> teleportEntityRandomly(entity, 8.0F));
        });
    }

    private void teleportEntityRandomly(Entity entity, float diameter) {
        ServerWorld world = (ServerWorld) entity.getWorld();
        for (int i = 0; i < 32; ++i) {
            double x = entity.getX() + (world.random.nextDouble() - 0.5) * diameter;
            double z = entity.getZ() + (world.random.nextDouble() - 0.5) * diameter;

            // Get the top Y coordinate for the generated X and Z
            int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, (int) x, (int) z);
            BlockPos topPos = BlockPos.ofFloored(x, topY, z);

            // Check if the block has collision and the block above is replaceable
            if (!world.getBlockState(topPos).getCollisionShape(world, topPos).isEmpty()
                    && world.getBlockState(topPos.up()).isReplaceable()) {

                Vec3d teleportVec = Vec3d.ofBottomCenter(topPos.up());
                Set<PositionFlag> flags = EnumSet.noneOf(PositionFlag.class);
                if (entity.teleport(world, teleportVec.x, teleportVec.y, teleportVec.z, flags, entity.getYaw(), entity.getPitch(), true)) {
                    world.playSound(null, teleportVec.x, teleportVec.y, teleportVec.z,
                            SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0F, 1.0F);
                    world.spawnParticles(ParticleTypes.PORTAL, teleportVec.x, teleportVec.y + 1, teleportVec.z, 10, 0.5, 0.5, 0.5, 0.05);
                    break;
                }
            }
        }
        entity.fallDistance = 0;
    }
}