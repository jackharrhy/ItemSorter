package tollenaar.stephen.ItemSorter.Events;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.InventoryHolder;

import tollenaar.stephen.ItemSorter.Core.Database;
import tollenaar.stephen.ItemSorter.Util.Server.Book;
import tollenaar.stephen.ItemSorter.Util.Server.Frame;
import tollenaar.stephen.ItemSorter.Util.Server.Hopper;

public class HopperHandler implements Listener {

    private final Database database;

    public HopperHandler(Database database) {
        this.database = database;
    }

    @EventHandler
    public void onHopperInputEvent(InventoryMoveItemEvent event) {
        boolean junctionCancelled = false;
        Location source = event.getSource().getLocation();
        if (source == null)
            return;
        Block hopper = source.getBlock();
        Location down = hopper.getRelative(BlockFace.DOWN).getLocation();

        // Handle hopper junctions and multiple items in the same inventory slot
        if (hopper.getType() == Material.HOPPER) {
            Integer hopperID = getHopperID(down);
            if (hopperID == null)
                return;

            Frame frame = getFrameByHopperID(hopperID);
            if (frame == null)
                return;

            Book book = Book.getBook(frame.getId());
            if (book == null)
                return;

            // Handle junctions and disabled hoppers
            if (Hopper.isJunction(hopper) &&
                    !down.equals(event.getDestination().getLocation())) {
                handleJunction(event, hopper, book);
                return;
            }

            // Check for ratio rules if applicable
            if (book.hasRatio() && Hopper.isJunction(hopper)) {
                if (!book.allowedMove(event.getDestination(), hopper)) {
                    event.setCancelled(true);
                    return;
                } else {
                    event.setCancelled(false);
                    junctionCancelled = false;
                }
            }

            // Standard item filtering and linked hopper logic
            handleStandardItemFiltering(event, hopper, book, junctionCancelled);
        }
    }

    private Integer getHopperID(Location location) {
        return (Integer) database.getSavedHopperByLocation(location, "id");
    }

    private Frame getFrameByHopperID(Integer hopperID) {
        return database.getSavedItemFrameByHopperID(hopperID, "id");
    }

    private void handleJunction(InventoryMoveItemEvent event, Block hopper, Book book) {
        if (book.allowItem(((InventoryHolder) hopper.getRelative(BlockFace.DOWN).getState()).getInventory(),
                event.getItem()) && !((org.bukkit.block.data.type.Hopper) hopper).isEnabled()) {
            event.setCancelled(true);
        }
    }

    private void handleStandardItemFiltering(InventoryMoveItemEvent event, Block hopper, Book book,
            boolean junctionCancelled) {
        Location destination = event.getDestination().getLocation();
        if (destination == null || destination.getBlock().getType() != Material.HOPPER)
            return;

        Integer hopperID = getHopperID(destination);
        if (hopperID == null)
            return;

        Frame frame = getFrameByHopperID(hopperID);
        if (frame == null) {
            database.deleteHopper(destination);
            return;
        }

        if (!book.allowItem(event.getDestination(), event.getItem())) {
            if (book.isLinkedBelow()) {
                handleLinkedHopperLogic(event, hopper, book);
            } else {
                event.setCancelled(true);
            }
        } else if (!junctionCancelled) {
            reverseRatioStep(event);
        }
    }

    private void handleLinkedHopperLogic(InventoryMoveItemEvent event, Block hopper, Book book) {
        Block linkedBlock = hopper;
        Book linkedBook = book;

        while (linkedBook.isLinkedBelow()) {
            Integer linkedHopperID = getHopperID(linkedBlock.getRelative(BlockFace.DOWN).getLocation());
            if (linkedHopperID == null)
                break;

            Frame linkedFrame = getFrameByHopperID(linkedHopperID);
            if (linkedFrame != null) {
                linkedBook = Book.getBook(linkedFrame.getId());
                if (linkedBook.allowItem(event.getDestination(), event.getItem())) {
                    return; // Allow item and exit if allowed in linked hopper
                } else {
                    linkedBlock = linkedBlock.getRelative(BlockFace.DOWN);
                }
            } else {
                break;
            }
        }
        // If linked hopper logic also fails to allow the item, cancel the event
        event.setCancelled(true);
    }

    private void reverseRatioStep(InventoryMoveItemEvent event) {
        Integer hopperID = getHopperID(event.getDestination().getLocation());
        if (hopperID == null)
            return;

        Frame frame = getFrameByHopperID(hopperID);
        if (frame == null)
            return;

        Book book = Book.getBook(frame.getId());
        if (book.hasRatio()) {
            book.reverseRatioStep();
        }
    }

    // this checks if the hopper can pick up an item and is not in strict mode.
    @EventHandler
    public void onHopperPickUpEvent(InventoryPickupItemEvent event) {
        Location location = event.getInventory().getLocation();
        if (location != null && location.getBlock().getType() == Material.HOPPER) {
            Integer hopperID = (Integer) database.getSavedHopperByLocation(
                    location,
                    "id");
            if (hopperID != null) {
                Frame frame = database.getSavedItemFrameByHopperID(hopperID, "id");
                if (frame != null) {
                    Book book = Book.getBook(frame.getId());
                    // checking the configuration
                    if (book != null && book.isStrictMode()) {
                        if (!book.allowItem(event.getInventory(), event.getItem().getItemStack())) {
                            event.setCancelled(true);
                        }
                    }
                }
            }
        }
    }
}
