package tollenaar.stephen.ItemSorter.Util.Server;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public final class Hopper {
    private static Map<Location, Hopper> HOPPERS = new HashMap<>();

    private final int id;
    private double x;
    private double y;
    private double z;
    private String world;

    private Hopper(ResultSet rs) throws SQLException {
        this(rs.getInt("id"), rs.getDouble("hopperX"), rs.getDouble("hopperY"), rs.getDouble("hopperZ"),
                rs.getString("hopperWorld"));
    }

    private Hopper(int id, double x, double y, double z, String world) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.world = world;
    }

    /**
     * Create a hopper from a database row and register it for lookup by location
     * or id. Registering here, after construction, keeps a half-built hopper from
     * escaping into the shared map.
     * 
     * @param rs - a row from the Hoppers table.
     * @return The registered hopper.
     * @throws SQLException when a column can't be read.
     */
    public static Hopper register(ResultSet rs) throws SQLException {
        Hopper hopper = new Hopper(rs);
        HOPPERS.put(new Location(Bukkit.getWorld(hopper.world), hopper.x, hopper.y, hopper.z), hopper);
        return hopper;
    }

    public int getId() {
        return id;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public String getWorld() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public Location getLocation() {
        return new Location(Bukkit.getWorld(getWorld()), getX(), getY(), getZ());
    }

    public Object getField(String field) {
        switch (field) {
            case "id" -> {
                return getId();
            }
            case "hopperX" -> {
                return getX();
            }
            case "hopperY" -> {
                return getY();
            }
            case "hopperZ" -> {
                return getZ();
            }
            case "hopperWorld" -> {
                return getWorld();
            }
            default -> throw new NullPointerException("Unknown Field");
        }
    }

    public static Hopper getHOPPER(Location location) {
        return HOPPERS.get(location);
    }

    public static Hopper getHOPPER(int id) {
        for (Location key : HOPPERS.keySet()) {
            if (HOPPERS.get(key).getId() == id) {
                return HOPPERS.get(key);
            }
        }
        return null;
    }

    public static void removeHOPPER(Location location) {
        HOPPERS.remove(location);
    }

    public static boolean isJunction(Block hopper) {
        BlockFace facing = ((org.bukkit.block.data.type.Hopper) hopper.getBlockData()).getFacing();
        if (facing != BlockFace.DOWN) {
            return hopper.getRelative(BlockFace.DOWN).getType() == Material.HOPPER
                    || hopper.getRelative(BlockFace.DOWN).getType() == Material.HOPPER_MINECART;
        }
        return false;
    }
}