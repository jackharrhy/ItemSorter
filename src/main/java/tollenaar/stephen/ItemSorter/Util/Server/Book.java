package tollenaar.stephen.ItemSorter.Util.Server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.data.type.Hopper;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import tollenaar.stephen.ItemSorter.Shared.FilterConfig;
import tollenaar.stephen.ItemSorter.Util.Text;
import tollenaar.stephen.ItemSorter.Util.Web.HopperItems;
import tollenaar.stephen.ItemSorter.Util.Web.Ratio;

public final class Book implements Serializable {

    private static final long serialVersionUID = 4519138450923555946L;
    private static final transient BiMap<Integer, Book> BOOKS = HashBiMap.create(); // mapped from frameID to

    // Hopper configurations are stored as this class's serialized form, so it must
    // not change. This names exactly the fields default serialization would write,
    // producing byte-identical output, and tells javac the List fields are meant to
    // be serialized. Keep it in sync when adding or removing a persisted field.
    private static final ObjectStreamField[] serialPersistentFields = {
            new ObjectStreamField("inputConfig", List.class),
            new ObjectStreamField("enchantments", List.class),
            new ObjectStreamField("potions", List.class),
            new ObjectStreamField("strictMode", boolean.class),
            new ObjectStreamField("preventOverflow", boolean.class),
            new ObjectStreamField("linkedBelow", boolean.class),
            new ObjectStreamField("ratio", Ratio.class) };

    public static ObjectStreamField[] getSerialPersistentFields() {
        return serialPersistentFields;
    }

    private List<Material> inputConfig = new ArrayList<>(); // input sorting configure
    private List<String> enchantments = new ArrayList<>();
    private List<String> potions = new ArrayList<>();
    private transient int frameID = -1;

    private boolean strictMode;
    private boolean preventOverflow;
    private boolean linkedBelow;
    private Ratio ratio;

    private Book() {}

    public Book(int frameID) {
        this.frameID = frameID;
        this.addSelf(frameID);
    }

    // adding to an item frame
    public void addSelf(int frameID) {
        this.frameID = frameID;
        BOOKS.put(frameID, this);
    }

    // check if the item is of the correct material
    public boolean hasInputConfig(Material material) {
        return inputConfig.contains(material);
    }

    // checking if the item has the correct enchantment
    public boolean hasEnchantment(Set<Enchantment> enchantment) {
        var names = enchantment.stream().map(ent -> ent.getKey().getKey()).collect(java.util.stream.Collectors.toSet());
        return toPortable().matchesEnchantments(names);
    }

    // checking if the item has the correct potion effect
    public boolean hasPotion(List<PotionEffect> potion) {
        return toPortable().matchesPotions(potion.stream().map(effect -> effect.getType().getKey().getKey()).toList());
    }

    public List<String> toPages() {
        List<String> pages = new ArrayList<>();

        // Material filtering
        String pathMaterials = "Material Filter";
        String pathEnchantments = "Enchantment Filter";
        String pathPotions = "Potion Filter";

        FileConfiguration pageBuilder = new YamlConfiguration();
        // tmp list for better formatting
        List<String> tmpMaterial = new ArrayList<>();
        List<String> tmpEnchantment = new ArrayList<>();
        List<String> tmpPotion = new ArrayList<>();
        for (Material material : this.inputConfig) {
            tmpMaterial.add(getDisplayName(material));
        }

        // Advance options
        pageBuilder.set("Strict Mode", isStrictMode());
        pageBuilder.set("Prevent Overflow", hasPreventOverflow());
        pageBuilder.set("Is Linked Below", isLinkedBelow());
        if (hasRatio()) {
            pageBuilder.set("Ratio", ratio.getFirst() + " To " + ratio.getSecond());
        } else {
            pageBuilder.set("Ratio", "None");
        }

        // Enchantment filtering
        for (String ent : enchantments) {
            tmpEnchantment.add(Text.capitalizeFully(ent.replace("_", " ")));
        }

        // Potion filtering
        for (String pot : potions) {
            tmpPotion.add(Text.capitalizeFully(pot.replace("_", " ")));
        }

        // setting the pageBuilder
        pageBuilder.set(pathMaterials, tmpMaterial);
        if (tmpEnchantment.isEmpty()) {
            pageBuilder.set(pathEnchantments, "None");
        } else {
            pageBuilder.set(pathEnchantments, tmpEnchantment);
        }
        if (!tmpPotion.isEmpty()) {
            pageBuilder.set(pathPotions, tmpPotion);
        } else {
            pageBuilder.set(pathPotions, "None");
        }

        // fitting the max amount of lines per page
        List<String> inputConfigList = new ArrayList<>(Arrays.asList(pageBuilder.saveToString().split("\n")));
        for (int i = 0; i < inputConfigList.size(); i += 13) {
            int maxSub = Math.min(inputConfigList.size() - i, 12);
            String page = inputConfigList.subList(i, i + maxSub).toString().replace("]", "").replace("[", "")
                    .replace(", ", "\n");
            if (maxSub == 12) {
                i--;
            }
            pages.add(page);
        }

        // Preventing a book that's too big
        if (pages.size() >= 100) {
            String[] lastPage = pages.get(99).split("\n");
            lastPage[lastPage.length - 1] = "...";
            pages.set(14, String.join("\n", lastPage));
            pages = pages.subList(0, 100);
        }

        return pages;
    }

    public boolean allowItem(Inventory inventory, ItemStack item) {
        var names = item.getEnchantments().keySet().stream().map(ent -> ent.getKey().getKey()).collect(java.util.stream.Collectors.toSet());
        var effects = item.getItemMeta() instanceof PotionMeta potion
                ? potion.getCustomEffects().stream().map(effect -> effect.getType().getKey().getKey()).toList()
                : List.<String>of();
        if (!toPortable().allows(item.getType().name().toLowerCase(java.util.Locale.ROOT), names, effects)) return false;

        // prevents accepting an item if the next container in the system is full
        if (hasPreventOverflow()) {
            Location location = inventory.getLocation();
            if (location == null) {
                return false;
            }
            Block hopper = location.getBlock();
            BlockFace facing = ((Hopper) hopper.getBlockData()).getFacing();
            Block target = hopper.getRelative(facing);
            if (target.getState() instanceof Container t) {
                if (!hasRoom(t.getInventory(), item)) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    private boolean hasRoom(Inventory inventory, ItemStack item) {
        return Arrays.stream(inventory.getStorageContents())
                .anyMatch(itemStack -> itemStack == null
                        || itemStack.getAmount() + item.getAmount() <= itemStack.getMaxStackSize());
    }

    // sorting the correct move detail
    public boolean allowedMove(Inventory destination, Block hopper) {
        BlockFace facing = ((Hopper) hopper.getBlockData()).getFacing();
        if (!ratio.allows(hopper.getRelative(facing).getLocation().equals(destination.getLocation()),
                hopper.getRelative(BlockFace.DOWN).getLocation().equals(destination.getLocation()))) return false;
        ratio.addCount();
        return true;
    }

    private String getDisplayName(Material material) {
        return Text.capitalizeFully(material.name().toLowerCase().replace("_", " "));
    }

    public void addInputConfig(Material material) {
        this.inputConfig.add(material);
    }

    public void addEnchantment(Enchantment enchantment) {
        this.enchantments.add(enchantment.getKey().getKey());
    }

    public void addPotion(PotionEffectType potion) {
        this.potions.add(potion.getKey().getKey());
    }

    public void emptyInputConfig() {
        this.inputConfig.clear();
        this.enchantments.clear();
        this.potions.clear();
    }

    public static boolean makePortable(ItemStack item, org.bukkit.NamespacedKey key) throws IOException, ClassNotFoundException {
        if (item == null || item.getType() != Material.WRITTEN_BOOK) return false;
        var meta = item.getItemMeta();
        String value = meta.getPersistentDataContainer().get(key, org.bukkit.persistence.PersistentDataType.STRING);
        if (value == null) return false;
        var book = fromString(value);
        meta.getPersistentDataContainer().set(key, org.bukkit.persistence.PersistentDataType.STRING, book.toPortable().encode());
        item.setItemMeta(meta);
        return true;
    }

    public FilterConfig toPortable() {
        return new FilterConfig(1,
                inputConfig.stream().map(material -> material.name().toLowerCase(java.util.Locale.ROOT)).toList(),
                enchantments, potions, strictMode, preventOverflow, linkedBelow,
                ratio == null ? null : ratio.getFirst(), ratio == null ? null : ratio.getSecond());
    }

    public HopperItems toItems() {
        List<String> items = new ArrayList<>();
        for (Material material : this.inputConfig) {
            items.add(material.name().toLowerCase());
        }

        HopperItems hopperItems = new HopperItems(items, enchantments, potions, isStrictMode(), hasPreventOverflow(),
                isLinkedBelow(),
                ratio);

        return hopperItems;
    }

    /** Write the object to a Base64 string. */
    @Override
    public String toString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos;
        try {
            oos = new ObjectOutputStream(baos);
            oos.writeObject(this);
            oos.close();
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.SEVERE, e.toString());
        }
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    public int getFrameID() {
        return frameID;
    }

    public boolean isStrictMode() {
        return strictMode;
    }

    public void setStrictMode(boolean strictMode) {
        this.strictMode = strictMode;
    }

    public boolean hasPreventOverflow() {
        return this.preventOverflow;
    }

    public void setPreventOverflow(boolean preventOverflow) {
        this.preventOverflow = preventOverflow;
    }

    public void setRatio(int first, int second) {
        this.ratio = new Ratio(first, second);
    }

    public void emptyRatio() {
        this.ratio = null;
    }

    public boolean hasRatio() {
        return this.ratio != null;
    }

    public void reverseRatioStep() {
        this.ratio.reverseCount();
    }

    // in case of a legacy load from string
    public void checkSelf() {
        if (this.inputConfig == null) {
            this.inputConfig = new ArrayList<>();
        }
        if (this.enchantments == null) {
            this.enchantments = new ArrayList<>();
        }
        if (this.potions == null) {
            this.potions = new ArrayList<>();
        }
    }

    public static Book getBook(int frameID) {
        return BOOKS.get(frameID);
    }

    public static Book getBook(String bookvalue) {
        for (Book book : BOOKS.values()) {
            if (book.toString().equals(bookvalue) || book.toPortable().encode().equals(bookvalue)) {
                return book;
            }
        }
        return null;
    }

    public static List<Book> getBook(List<Integer> frameIDs) {
        List<Book> tmp = new ArrayList<>();
        for (int frameID : frameIDs) {
            if (getBook(frameID) != null) {
                tmp.add(BOOKS.get(frameID));
            }
        }
        return tmp;
    }

    public static void removeBook(int frameID) {
        BOOKS.remove(frameID);
    }

    public static Book fromPortable(FilterConfig filter) {
        Book book = new Book();
        for (String item : filter.items()) {
            Material material = Material.matchMaterial(item);
            if (material == null) throw new IllegalArgumentException("Unknown material");
            book.addInputConfig(material);
        }
        book.enchantments.addAll(filter.enchantments());
        book.potions.addAll(filter.potions());
        book.strictMode = filter.strictMode();
        book.preventOverflow = filter.preventOverflow();
        book.linkedBelow = filter.linkedBelow();
        if (filter.firstRatio() != null) book.setRatio(filter.firstRatio(), filter.secondRatio());
        return book;
    }

    public ItemStack toItem(org.bukkit.NamespacedKey key) {
        ItemStack item = new ItemStack(Material.WRITTEN_BOOK);
        var meta = (org.bukkit.inventory.meta.BookMeta) item.getItemMeta();
        meta.setTitle("HopperConfiguration");
        meta.setAuthor("ItemSorter");
        meta.pages(Text.legacy(toPages()));
        meta.getPersistentDataContainer().set(key, org.bukkit.persistence.PersistentDataType.STRING, toPortable().encode());
        item.setItemMeta(meta);
        return item;
    }

    /** Read the object from Base64 string. */
    public static Book fromString(String s) throws IOException, ClassNotFoundException {
        if (s.startsWith("{")) return fromPortable(FilterConfig.decode(s));
        byte[] data = Base64.getDecoder().decode(s);
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
        Object o;
        try {
            o = ois.readObject();
            ois.close();

        } catch (ClassNotFoundException e) {
            ois.close();
            try (LegacyObjectInputStream lois = new LegacyObjectInputStream(new ByteArrayInputStream(data),
                    "tollenaar.stephen.ItemSorter.Util.Book", "tollenaar.stephen.ItemSorter.Util.Server.Book")) {
                try {
                    o = lois.readObject();
                } catch (IOException ex) {
                    return null;
                }
            }
            ((Book) o).checkSelf();
        }
        return (Book) o;
    }

    public boolean isLinkedBelow() {
        return linkedBelow;
    }

    public void setLinkedBelow(boolean linkedBelow) {
        this.linkedBelow = linkedBelow;
    }
}

class LegacyObjectInputStream extends ObjectInputStream {
    private final String oldNameSpace;
    private final String newNameSpace;

    public LegacyObjectInputStream(InputStream in, String oldNameSpace, String newNameSpace) throws IOException {
        super(in);
        this.oldNameSpace = oldNameSpace;
        this.newNameSpace = newNameSpace;
    }

    @Override
    protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
        ObjectStreamClass result = super.readClassDescriptor();
        try {
            if (result.getName().contains(oldNameSpace)) {
                String newClassName = result.getName().replace(oldNameSpace, newNameSpace);
                // Test the class exists
                Class<?> localClass = Class.forName(newClassName);

                Field nameField = ObjectStreamClass.class.getDeclaredField("name");
                nameField.setAccessible(true);
                nameField.set(result, newClassName);

                ObjectStreamClass localClassDescriptor = ObjectStreamClass.lookup(localClass);
                Field suidField = ObjectStreamClass.class.getDeclaredField("suid");
                suidField.setAccessible(true);
                suidField.set(result, localClassDescriptor.getSerialVersionUID());
            }
        } catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException | NoSuchFieldException
                | SecurityException e) {
            throw new IOException("Exception when trying to replace namespace", e);
        }
        return result;
    }
}
