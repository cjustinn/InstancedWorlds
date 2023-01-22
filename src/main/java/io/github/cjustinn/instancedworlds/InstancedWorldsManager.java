package io.github.cjustinn.instancedworlds;

import io.github.cjustinn.instancedworlds.CustomItems.CustomItem;
import io.github.cjustinn.instancedworlds.CustomItems.LootTable;
import io.github.cjustinn.instancedworlds.Instances.InstanceActionType;
import io.github.cjustinn.instancedworlds.Instances.InstancePortal;
import io.github.cjustinn.instancedworlds.Instances.InstantiatedWorld;
import io.github.cjustinn.instancedworlds.Parties.Party;
import io.github.cjustinn.instancedworlds.Parties.PartyInvite;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

public class InstancedWorldsManager {

    // Configuration Settings
    public static int maxPartySize = 5;
    public static int partyInviteTimeout = 60;
    public static int instancePortalCooldown = 15;

    // Static lists used to store persisted data.
    // Instance-related lists
    public static List<World> templates = new ArrayList<World>();
    public static List<InstantiatedWorld> instances = new ArrayList<InstantiatedWorld>();
    public static List<InstancePortal> portals = new ArrayList<InstancePortal>();

    // Party-related lists
    public static List<Party> parties = new ArrayList<Party>();
    public static List<PartyInvite> partyInvites = new ArrayList<PartyInvite>();

    // Custom Item-related lists.
    public static List<CustomItem> customItems = new ArrayList<>();
    public static List<LootTable> lootTables = new ArrayList<>();

    // Function definitions
    /*
        Pass a "String" loot table id and a "List<String>" of items, which the function will
        use to create a new LootTable object and store it in the "lootTables" list.
    */
    public static void registerLootTable(String id, List<String> items) {
        InstancedWorldsManager.lootTables.add(new LootTable(id, items));
    }

    /*
        Pass a "String" loot table id for the function to use to iterate through all of them,
        returning true or false, depending on if a loot table with the same id exists.
    */
    public static boolean lootTableExists(String id) {
        boolean found = false;

        for (int i = 0; i < InstancedWorldsManager.lootTables.size() && !found; i++) {
            if (InstancedWorldsManager.lootTables.get(i).getTableId().equalsIgnoreCase(id))
                found = true;
        }

        return found;
    }

    /*
        Pass a "String" type custom loot table id value, which the function uses
        whilst iterating through all registered loot tables to find and return the
        associated loot table, or null if it finds nothing.
    */
    public static LootTable getLootTableById(String id) {
        boolean found = false;
        LootTable lootTable = null;

        for (int i = 0; i < InstancedWorldsManager.lootTables.size() && !found; i++) {
            LootTable table = InstancedWorldsManager.lootTables.get(i);

            if (table.getTableId().equalsIgnoreCase(id)) {
                found = true;
                lootTable = table;
            }
        }

        return lootTable;
    }

    /*
        Pass a "String" item id, "String" item name, "int" custom model data, "List<String>"
        lore, "String" item material type, and a "Map<String, Integer>" of enchantments,
        which the function will use to create and save a new CustomItem.
    */
    public static void registerCustomItem(String id, String name, String type, int cmd, List<String> lore, Map<String, Integer> enchants) {
        InstancedWorldsManager.customItems.add(new CustomItem(id, name, cmd, lore, type, enchants));
    }

    /*
        Pass a "String" type custom item id value, which the function uses whilst
        iterating through all registered custom items to find and return the associated
        custom item, or null if it finds nothing.
    */
    public static CustomItem getCustomItemById(String id) {
        boolean found = false;
        CustomItem item = null;

        for (int i = 0; i < InstancedWorldsManager.customItems.size() && !found; i++) {
            CustomItem current = InstancedWorldsManager.customItems.get(i);
            if (current.getItemId().equalsIgnoreCase(id)) {
                found = true;
                item = current;
            }
        }

        return item;
    }

    /*
        Pass a "String" type world name value, which the function will use to check if a
        template world exists with that name.
    */
    public static boolean templateExistsByName(String name) {
        boolean exists = false, found = false;

        if (!name.startsWith("template_"))
            name = String.format("template_%s", name);

        for (int i = 0; i < InstancedWorldsManager.templates.size() && !found; i++) {
            if (InstancedWorldsManager.templates.get(i).getName().equalsIgnoreCase(name)) {
                exists = true;
                found = true;
            }
        }

        return exists;
    }

    /*
        Pass a "Location" object representing a player's current position,
        which the function uses to check if it's within the region defining
        any existing portals.
    */
    public static InstancePortal playerTouchedPortal(Location playerLocation) {
        boolean found = false;
        InstancePortal touched = null;

        for (int i = 0; i < InstancedWorldsManager.portals.size() && !found; i++) {
            if (InstancedWorldsManager.portals.get(i).getRegion().contains(playerLocation)) {
                found = true;
                touched = InstancedWorldsManager.portals.get(i);
            }
        }

        return touched;
    }

    /*
        Pass an "InstancePortal" object to the function, and it will save it into the
        portals list.
    */
    public static void savePortal(InstancePortal portal) {
        InstancedWorldsManager.portals.add(portal);
    }

    /*
        Pass a "UUID" player id value and a "String" template name, which the function will
        use to find an existing instance with matching values, and destroy the instanced world.
    */
    public static boolean removeInstance(UUID player, String template) {
        boolean success = true;

        final int index = InstancedWorldsManager.getPlayerInstanceIndex(player, template);
        if (index < 0) success = false;
        else {

            if (InstancedWorldsManager.saveConfigValue("instances." + InstancedWorldsManager.instances.get(index).getInstanceId(), null)) {
                InstancedWorldsManager.instances.get(index).destroyInstance();
                InstancedWorldsManager.instances.remove(index);
            } else {
                Bukkit.getConsoleSender().sendMessage(String.format("[InstancedWorlds] %sThe instance [%s] could not be destroyed.", ChatColor.RED, InstancedWorldsManager.instances.get(index).getInstanceId()));
                success = false;
            }

        }

        return success;
    }

    /*
        Pass a "String" template id value, which the function will
        use to iterate through all existing instances and return the int-value index of the
        element in the "instances" list.
    */
    public static int getPlayerInstanceIndex(String instanceId) {
        int index = -1;
        boolean found = false;

        for (int i = 0; i < InstancedWorldsManager.instances.size() && !found; i++) {
            if (InstancedWorldsManager.instances.get(i).getInstanceId().equals(instanceId)) {
                index = i;
                found = true;
            }
        }

        return index;
    }

    /*
        Pass a "UUID" player id value and a "String" template name, which the function will
        use to iterate through all existing instances and return the int-value index of the
        element in the "instances" list.
    */
    public static int getPlayerInstanceIndex(UUID player, String template) {
        int index = -1;
        boolean found = false;

        for (int i = 0; i < InstancedWorldsManager.instances.size() && !found; i++) {
            if (InstancedWorldsManager.instances.get(i).isOwner(player) && InstancedWorldsManager.instances.get(i).getTemplateName().toLowerCase().equals(template.toLowerCase())) {
                index = i;
                found = true;
            }
        }

        return index;
    }

    /*
        Pass an "InstantiatedWorld" instance to the function, which it will then
        store in the "instances" list.
    */
    public static void saveInstance(InstantiatedWorld instance) {
        InstancedWorldsManager.instances.add(instance);
    }

    /*
        Pass a "UUID" player id value and a "String" template name, which the function will
        use to iterate through all existing instances and return a boolean indicating if the
        passed player already has an existing instance of the provided template created.
    */
    public static boolean playerHasExistingInstance(UUID player, String template) {
        boolean found = false, exists = false;

        for (int i = 0; i < instances.size() && !found; i++) {
            if (InstancedWorldsManager.instances.get(i).getTemplateName().toLowerCase().equals(template.toLowerCase()) && InstancedWorldsManager.instances.get(i).isOwner(player)) {
                found = true;
                exists = true;
            }
        }

        return exists;
    }

    /*
        Pass two "Player" objects, which the function will use to create a new party
        invite that the "recipient" player can use to join the party led by the
        "sender".
    */
    public static void invitePlayerToParty(Player recipient, Player sender) {
        if (!InstancedWorldsManager.playerIsLeadingParty(sender.getUniqueId())) {
            sender.sendMessage(ChatColor.RED + "You are not the leader of a party!");
            return;
        }

        // Get the sender's party.
        final int partyIndex = InstancedWorldsManager.getPlayerPartyIndex(sender.getUniqueId());
        if (partyIndex > -1) {
            Party senderParty = InstancedWorldsManager.parties.get(partyIndex);

            // Create a new invite object.
            PartyInvite invite = new PartyInvite(senderParty, recipient.getUniqueId());

            InstancedWorldsManager.partyInvites.add(invite);

            sender.sendMessage(ChatColor.GREEN + recipient.getName() + " has been invited to your party.");
            recipient.sendMessage(ChatColor.GOLD + "You have been invited to " + sender.getName() + "'s party. Use the '/party join " + sender.getName() + "' command to accept the invite.");
        } else {
            sender.sendMessage(ChatColor.RED + "There was an error inviting the player to your party. Please try again!");
        }
    }

    /*
        Pass a "Player" object, which the function will use to create a new party
        and store it in the "parties" list.
    */
    public static boolean createParty(Player founder) {
        boolean success = false;

        if (!InstancedWorldsManager.playerIsInParty(founder.getUniqueId())) {
            Party party = new Party(new ArrayList<>(Arrays.asList(founder)), founder);

            InstancedWorldsManager.parties.add(party);

            success = true;
        }

        return success;
    }

    /*
        Pass a "Player" object, which the function will use to remove the player from
        the party that they are current a member of.
    */
    public static boolean leaveParty(Player player) {
        boolean success = true;

        if (!InstancedWorldsManager.playerIsInParty(player.getUniqueId()))
            success = false;
        else {

            // Player IS in a party.
            final int partyIndex = InstancedWorldsManager.getPlayerPartyIndex(player.getUniqueId());
            if (partyIndex >= 0) {

                Party party = InstancedWorldsManager.parties.get(partyIndex);
                party.removePlayerFromParty(player);

                if (party.getMemberCount() == 0) {
                    InstancedWorldsManager.removeInvitesForParty(party.getPartyId());
                    InstancedWorldsManager.parties.get(partyIndex).disableListener();
                    InstancedWorldsManager.parties.remove(partyIndex);
                } else {
                    InstancedWorldsManager.parties.set(partyIndex, party);
                }

            } else success = false;

        }

        return success;
    }

    public static void removeInvitesForParty(UUID partyId) {
        for (int i = 0; i < InstancedWorldsManager.partyInvites.size(); i++) {
            if (InstancedWorldsManager.partyInvites.get(i).getParty().equals(partyId))
                InstancedWorldsManager.partyInvites.remove(i);
        }
    }

    /*
        Pass a "String" name, which the function will use to check if there is an existing
        portal with the provided name and return the result (true / false).
    */
    public static boolean portalExistsWithName(String name) {
        return InstancedWorldsManager.getPortalIndexByName(name) >= 0;
    }

    /*
        Pass a "String" name, which the function will use to find and return the index of the
        portal which has a matching name attached to it.
    */
    public static int getPortalIndexByName(String name) {
        int index = -1;
        boolean found = false;

        for(int i = 0; i < InstancedWorldsManager.portals.size() && !found; i++) {
            if (InstancedWorldsManager.portals.get(i).getName().equalsIgnoreCase(name)) {
                found = true;
                index = i;
            }
        }

        return index;
    }

    /*
        Pass a "UUID" to the function, and it will check all existing parties and return the
        party that the provided user is part of.
    */
    public static int getPlayerPartyIndex(UUID player) {
        boolean found = false;
        int index = -1;

        for (int i = 0; i < InstancedWorldsManager.parties.size() && !found; i++) {
            if (InstancedWorldsManager.parties.get(i).playerIsInParty(player)) {
                found = true;
                index = i;
            }
        }

        return index;
    }

    /*
        Pass a "UUID" to the function, and it will check all existing parties to see
        if the provided user is a member of any of them.
    */
    public static boolean playerIsInParty(UUID ply) {
        boolean inParty = false, found = false;

        for (int i = 0; i < InstancedWorldsManager.parties.size() && !found; i++) {
            if (InstancedWorldsManager.parties.get(i).playerIsInParty(ply)) {
                inParty = true;
                found = true;
            }
        }

        return inParty;
    }

    /*
        Pass a "Player" object and a String to the function, and it will find the party invite
        and attempt to add the player to the party, if possible.
    */
    public static boolean joinParty(Player player, String partyLeader) {
        boolean success = true;

        final int inviteIndex = InstancedWorldsManager.getPartyInviteIndex(player.getUniqueId(), partyLeader);
        if (inviteIndex < 0) {
            player.sendMessage(ChatColor.RED + (InstancedWorldsManager.playerIsLeadingParty(partyLeader) ? "You have not been invited to that party!" : "That player is not currently the leader of a party!"));
            success = false;
        } else {

            // The invite is valid and could be found.
            PartyInvite invite = InstancedWorldsManager.partyInvites.get(inviteIndex);
            if (invite.inviteIsExpired()) {
                player.sendMessage(ChatColor.RED + "That invite has expired!");
                success = false;
            } else {

                // The invite is NOT expired.
                final int partyIndex = InstancedWorldsManager.getPartyIndex(InstancedWorldsManager.partyInvites.get(inviteIndex).getParty());
                if (partyIndex < 0) {
                    player.sendMessage(ChatColor.RED + "That party no longer exists!");
                    success = false;
                } else {

                    // The party DOES still exist.
                    Party party = InstancedWorldsManager.parties.get(partyIndex);
                    if (party.partyIsFull()) {
                        player.sendMessage(ChatColor.RED + "That party is full!");
                        success = false;
                    } else {

                        // The party is NOT full. The player can now be added.
                        party.broadcastMessageToMembers(ChatColor.GREEN + player.getName() + " has joined the party!");

                        party.addPlayerToParty(player);
                        InstancedWorldsManager.parties.set(partyIndex, party);

                        player.sendMessage(ChatColor.GREEN + "You have joined the party!");

                    }

                }

            }

            InstancedWorldsManager.partyInvites.remove(inviteIndex);

        }

        return success;
    }

    /*
        Pass a "UUID" and the function will use it to find if a party currently exists with
        the target user as its leader.
    */
    public static boolean playerIsLeadingParty(UUID ply) {
        boolean leader = false, found = false;

        for (int i = 0; i < InstancedWorldsManager.parties.size() && !found; i++) {
            if (InstancedWorldsManager.parties.get(i).getLeader().getUniqueId().equals(ply)) {
                leader = true;
                found = true;
            }
        }

        return leader;
    }

    /*
        Pass a "String" and the function will use it to find if a party currently exists with
        the target user as its leader.
    */
    public static boolean playerIsLeadingParty(String ply) {
        boolean leader = false, found = false;

        for (int i = 0; i < InstancedWorldsManager.parties.size() && !found; i++) {
            if (InstancedWorldsManager.parties.get(i).getLeader().getName().toLowerCase().equals(ply.toLowerCase())) {
                leader = true;
                found = true;
            }
        }

        return leader;
    }

    /*
        Pass a "UUID" and a string, and the function will find the party invite with
        the corresponding details and return its index.
    */
    public static int getPartyInviteIndex(UUID player, String leader) {
        int index = -1;
        boolean found = false;

        for (int i = 0; i < InstancedWorldsManager.partyInvites.size() && !found; i++) {
            if (InstancedWorldsManager.partyInvites.get(i).partyIsMatch(player, leader)) {
                index = i;
                found = true;
            }
        }

        return index;
    }

    /*
        Pass a "UUID" value for the function to use to find the corresponding
        party in the "parties" list.
    */
    public static int getPartyIndex(UUID partyId) {
        int index = -1;
        boolean found = false;

        for (int i = 0; i < InstancedWorldsManager.parties.size() && !found; i++) {
            if (InstancedWorldsManager.parties.get(i).getPartyId().equals(partyId)) {
                index = i;
                found = true;
            }
        }

        return index;
    }



    /*
        Pass a "World" object to the function, which it will use to update the config file to
        include the new template world as well as adding it to the list of template worlds
        used by the plugin.
    */
    public static boolean saveTemplateWorld(World template) {
        boolean success = true;

        InstancedWorlds plugin = (InstancedWorlds) Bukkit.getPluginManager().getPlugin("InstancedWorlds");
        if (plugin == null) success = false;
        else {
            // Add the template world to the list.
            InstancedWorldsManager.templates.add(template);

            // Save the template name to the config file "templates" section.
            plugin.getConfigurationFile().set("templates", InstancedWorldsManager.getTemplateNames());
            plugin.saveConfig();
        }

        return success;
    }

    public static void addTemplate(World template) {
        InstancedWorldsManager.templates.add(template);
    }

    /*
        Takes a "String" value as the config path, and any type of data to be saved as a
        configuration value.
    */
    public static boolean saveConfigValue(String path, Object value) {
        boolean success = true;

        InstancedWorlds plugin = (InstancedWorlds) Bukkit.getPluginManager().getPlugin("InstancedWorlds");
        if (plugin == null) success = false;
        else {

            plugin.getConfigurationFile().set(path, value);
            plugin.saveConfig();

        }

        return success;
    }

    /*
        Requires no parameters, this plugin is just used to convert the template worlds list into a
        list of only their world names, which can then be saved as a list in the config file.
    */
    public static List<String> getTemplateNames() {
        List<String> names = new ArrayList<String>();

        for (World template : InstancedWorldsManager.templates) {
            names.add(template.getName());
        }

        return names;
    }

    /*
        Pass the name of the world (without the prefix) to the function, which it uses
        as it iterates through the entire "templates" list comparing the passed name,
        with the prefix added, and returning either null (if no match was found) or
        the "World" object for the target template world.
    */
    public static World findTemplateByName(String name) {
        final String templateName = "template_" + name;
        World template = null;
        boolean found = false;

        for (int i = 0; i < templates.size() && !found; i++) {
            if (templates.get(i).getName().equals(templateName)) {
                template = templates.get(i);
                found = true;
            }
        }

        return template;
    }

}
