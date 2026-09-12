package tollenaar.stephen.ItemSorter.Util.Server;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;

public final class Frame {
    private static Map<Location, Frame> FRAMES = new HashMap<>();

    private final int id;
    private int hopperID;
    private double x;
    private double y;
    private double z;
    private float pitch;
    private float yaw;
    private String world;

    private Frame(ResultSet rs) throws SQLException {
        this(rs.getInt("id"), rs.getInt("hopper_id"), rs.getDouble("frameX"), rs.getDouble("frameY"),
                rs.getDouble("frameZ"), rs.getFloat("frameYaw"), rs.getFloat("framePitch"), rs.getString("frameWorld"));
    }

    private Frame(int id, int hopperID, double x, double y, double z, float yaw, float pitch, String world) {
        this.id = id;
        this.hopperID = hopperID;
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = pitch;
        this.yaw = yaw;
        this.world = world;
    }

    /**
     * Create a frame from a database row and register it for lookup by location
     * or id. Registering here, after construction, keeps a half-built frame from
     * escaping into the shared map.
     * 
     * @param rs - a row from the Frames table.
     * @return The registered frame.
     * @throws SQLException when a column can't be read.
     */
    public static Frame register(ResultSet rs) throws SQLException {
        Frame frame = new Frame(rs);
        FRAMES.put(new Location(Bukkit.getWorld(frame.world), frame.x, frame.y, frame.z, frame.yaw, frame.pitch),
                frame);
        return frame;
    }

    public int getId() {
        return id;
    }

    public int getHopperID() {
        return hopperID;
    }

    public void setHopperID(int hopperID) {
        this.hopperID = hopperID;
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

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public Location getLocation() {
        return new Location(Bukkit.getWorld(getWorld()), getX(), getY(), getZ(), getYaw(), getPitch());
    }

    public Object getField(String field) {
        switch (field) {
            case "id" -> {
                return getId();
            }
            case "frameX" -> {
                return getX();
            }
            case "frameY" -> {
                return getY();
            }
            case "frameZ" -> {
                return getZ();
            }
            case "frameWorld" -> {
                return getWorld();
            }
            case "framePitch" -> {
                return getPitch();
            }
            case "frameYaw" -> {
                return getYaw();
            }
            case "hopper_id" -> {
                return getHopperID();
            }
            default -> throw new NullPointerException("Unknown Field");
        }
    }

    public ItemFrame getEntityFrame() {
        Location frameLoc = new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
        if (frameLoc.getWorld() != null) {
            for (Entity ent : frameLoc.getChunk().getEntities()) {
                // getting the right item frame
                if (ent instanceof ItemFrame && ent.getLocation().equals(frameLoc)) {
                    return (ItemFrame) ent;
                }
            }
        }
        return null;
    }

    public static Frame getFRAME(Location location) {
        return FRAMES.get(location);
    }

    public static Frame getFRAME(int id) {
        for (Location key : FRAMES.keySet()) {
            if (FRAMES.get(key).getId() == id) {
                return FRAMES.get(key);
            }
        }
        return null;
    }

    public static Set<Location> getFrames() {
        return FRAMES.keySet();
    }

    public static void removeFRAME(Location location) {
        FRAMES.remove(location);
    }
}
