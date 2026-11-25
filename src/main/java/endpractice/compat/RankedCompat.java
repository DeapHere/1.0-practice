package endpractice.compat;

import java.lang.reflect.Field;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Running next to Oldschool Ranked. When ranked is doing anything (queued, in a private room, loading, in a match or
 * on its result) OldschoolPrac stays out of the way: no practice modes, commands, keys, overlays or HUD. Ranked is
 * read by reflection, so OldschoolPrac doesn't need it installed.
 */
public final class RankedCompat {
    private static boolean looked;
    private static Field state;

    private RankedCompat() {
    }

    private static void look() {
        looked = true;
        try {
            if (!FabricLoader.getInstance().isModLoaded("oldschoolranked")) return;
            state = Class.forName("oldschoolranked.Ranked").getField("state");
        } catch (Throwable t) {
            state = null;
            System.out.println("[OldschoolPrac] Oldschool Ranked is installed but its state couldn't be read: " + t);
        }
    }

    /** Whether Oldschool Ranked is installed at all. */
    public static boolean installed() {
        if (!looked) look();
        return state != null;
    }

    /** Ranked is busy: anything but idle. */
    public static boolean busy() {
        if (!looked) look();
        if (state == null) return false;
        try {
            Object s = state.get(null);
            return s != null && !"IDLE".equals(((Enum<?>) s).name());
        } catch (Throwable t) {
            return false;
        }
    }
}
