package endpractice.mixin;

import endpractice.EndPractice;
import net.minecraft.client.render.entity.EndermanRenderer;
import net.minecraft.entity.mob.monster.EndermanEntity;
import net.minecraft.util.math.Box;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Enderman outline setting: every enderman's hitbox drawn as a box, seen through blocks, in practice worlds. Aggro dot
 * setting: a red dot above every enderman that is after you.
 */
@Mixin(EndermanRenderer.class)
public abstract class EndermanRendererMixin {
    @Inject(method = "render(Lnet/minecraft/entity/mob/monster/EndermanEntity;DDDFF)V", at = @At("TAIL"), require = 0)
    private void endpractice$outline(EndermanEntity e, double dx, double dy, double dz, float yaw, float tickDelta, CallbackInfo ci) {
        if (EndPractice.cfg == null || !EndPractice.inPracticeWorld()) return;
        boolean outline = EndPractice.cfg.endermanOutline;
        // angry: it has a target (you), the same flag vanilla uses for its open mouth
        boolean dot = EndPractice.cfg.endermanAggroDot && e.angry;
        if (!outline && !dot) return;
        Box b = e.shape;
        double g = 0.05;
        double x0 = b.minX - e.x + dx - g, y0 = b.minY - e.y + dy - g, z0 = b.minZ - e.z + dz - g;
        double x1 = b.maxX - e.x + dx + g, y1 = b.maxY - e.y + dy + g, z1 = b.maxZ - e.z + dz + g;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LINE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_POINT_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_FOG);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        if (outline) {
            GL11.glLineWidth(3.0F);
            GL11.glColor4f(1.0F, 0.3F, 1.0F, 1.0F);
            GL11.glBegin(GL11.GL_LINES);
            double[][] c = {{x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}, {x0, y0, z1}, {x0, y1, z0}, {x1, y1, z0}, {x1, y1, z1}, {x0, y1, z1}};
            int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
            for (int[] ed : edges) {
                GL11.glVertex3d(c[ed[0]][0], c[ed[0]][1], c[ed[0]][2]);
                GL11.glVertex3d(c[ed[1]][0], c[ed[1]][1], c[ed[1]][2]);
            }
            GL11.glEnd();
        }
        if (dot) {
            // a round red dot half a block above its head, the same size on screen at any distance
            GL11.glEnable(GL11.GL_POINT_SMOOTH);
            GL11.glPointSize(14.0F);
            GL11.glColor4f(1.0F, 0.1F, 0.1F, 1.0F);
            GL11.glBegin(GL11.GL_POINTS);
            GL11.glVertex3d((x0 + x1) / 2, y1 + 0.5, (z0 + z1) / 2);
            GL11.glEnd();
        }
        GL11.glPopAttrib();
    }
}
