package io.github.cjustinn.instancedworlds;

import io.github.cjustinn.instancedworlds.CustomItems.CustomItem;
import io.github.cjustinn.instancedworlds.CustomItems.LootTable;
import io.github.cjustinn.instancedworlds.Instances.Actions.*;
import io.github.cjustinn.instancedworlds.Instances.InstancePortal;
import io.github.cjustinn.instancedworlds.Instances.InstanceTemplate;
import io.github.cjustinn.instancedworlds.Instances.InstantiatedWorld;
import io.github.cjustinn.instancedworlds.Parties.Party;
import io.github.cjustinn.instancedworlds.Parties.PartyInvite;
import io.github.cjustinn.instancedworlds.Summoning.SummoningInvite;
import io.github.cjustinn.instancedworlds.Summoning.SummoningStone;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class InstancedWorldsManager {

    // Configuration Settings
    public static int maxPartySize = 5;
    public static int partyInviteTimeout = 60;
    public static int instancePortalCooldown = 15;
    public static int summonInviteTimeout = 30;
    public static int summonStoneUseRange = 5;

    // Static lists used to store persisted data.
    // Instance-related lists
    public static List<InstanceTemplate> templates = new ArrayList<InstanceTemplate>();
    public static List<InstantiatedWorld> instances = new ArrayList<InstantiatedWorld>();
    public static List<InstancePortal> portals = new ArrayList<InstancePortal>();

    // Party-related lists
    public static List<Party> parties = new ArrayList<Party>();
    public static List<PartyInvite> partyInvites = new ArrayList<PartyInvite>();

    // Custom Item-related lists.
    public static List<CustomItem> customItems = new ArrayList<>();
    public static List<LootTable> lootTables = new ArrayList<>();

    // Summoning-related lists.
    public static List<SummoningStone> summoningStones = new ArrayList<>();
    public static List<SummoningInvite> summoningInvites = new ArrayList<>();

    // Action Mapping
    public static Map<String, Function<Sign, Action>> actionMaps = new HashMap<String, Function<Sign, Action>>() {{
        put("spawnmob", (Sign sign) -> {
            // Get the type of entity from the third line.
            String targetEntity = ((TextComponent) sign.line(2)).content();

            boolean isMythicMob = targetEntity.toLowerCase().startsWith("mm:");
            int amount = 0, mobId = -1, radius = -1;

            // Use the final line to extract any necessary data for spawning conditions, such as radius and the mob id.
            String[] splitValues = ((TextComponent) sign.line(3)).content().split(";");

            /*
                Different things should happen, depending on the values provided by the user.

                1 Value     -       The value should be considered the AMOUNT of the mob to spawn.
                2 Values    -       The values should be considered the AMOUNT of the mob to spawn, and the
                                        RADIUS the player has to be in, from the spawn location, in order
                                        for the mob(s) to be spawned.
                3 Values    -       The values should be considered the AMOUNT of the mob to spawn, the
                                        ID that should be assigned to the mob(s), and the RADIUS the player
                                        has to be in, from the spawn location, in order for the mob(s) to be
                                        spawned.
            */
            switch(splitValues.length) {
                case 1:
                    amount = InstancedWorldsManager.parseStringToInt(splitValues[0], 0);
                    break;
                case 2:
                    amount = InstancedWorldsManager.parseStringToInt(splitValues[0], 0);
                    radius = InstancedWorldsManager.parseStringToInt(splitValues[1], -1);
                    break;
                case 3:
                    amount = InstancedWorldsManager.parseStringToInt(splitValues[0], 0);
                    mobId = InstancedWorldsManager.parseStringToInt(splitValues[1], -1);
                    radius = InstancedWorldsManager.parseStringToInt(splitValues[2], -1);
                    break;
                default:
                    break;
            }

            /*
                If the sign indicates that it should be a MythicMob that is spawned, create a SpawnMythicMobAction provided that the
                MythicMobs plugin is enabled on the server. If it isn't marked with the "mm:" tag, then consider
                it to be a vanilla minecraft mob and create a SpawnMobAction.
            */
            if (isMythicMob) {
                if (InstancedWorldsManager.isPluginEnabled("MythicMobs")) {
                    return new SpawnMythicMobAction(targetEntity.replace("mm:", ""), amount, mobId, radius, sign.getLocation());
                }
            } else {
                EntityType mob = EntityType.fromName(targetEntity.toLowerCase());
                if (mob != null) {
                    return new SpawnMobAction(mob, amount, radius, mobId, sign.getLocation());
                }
            }

            return null;
        });
        put("spawnloot", (Sign sign) -> {
            // Get the loot table id from the third line.
            String targetLootTable = ((TextComponent) sign.line(2)).content();

            // Check if the loot table exists.
            if (InstancedWorldsManager.lootTableExists(targetLootTable)) {

                // Get the mob id number to watch for from the final line of the sign.
                int targetMob = InstancedWorldsManager.parseStringToInt(((TextComponent) sign.line(3)).content(), -1);

                // Create and return a new SpawnLootAction object.
                return new SpawnLootAction(targetMob, targetLootTable, sign.getLocation());

            }

            return null;
        });
        put("teleport", (Sign sign) -> {
            // Get the teleport location from the third line of the sign.
            String coordinatesLine = ((TextComponent) sign.line(2)).content();
            String[] coordinateData = coordinatesLine.split(";");

            // Attempt to create the target "Location" object.
            Location target = null;

            if (coordinateData.length >= 3) {
                if (InstancedWorldsManager.valueIsNumeric(coordinateData[0]) && InstancedWorldsManager.valueIsNumeric(coordinateData[1]) && InstancedWorldsManager.valueIsNumeric(coordinateData[2])) {
                    target = new Location(sign.getWorld(), Double.parseDouble(coordinateData[0]), Double.parseDouble(coordinateData[1]), Double.parseDouble(coordinateData[2]));
                }
            }

            // Proceed only if the target was created successfully.
            if (target != null) {

                // Get the content of the final line, which should contain radius and potentially a mob id.
                String conditionalsLine = ((TextComponent) sign.line(3)).content();
                String[] conditionals = conditionalsLine.split(";");

                // Define and initialize the necessary data variables with the conditional values.
                int radius = 1, mobId = -1;

                switch(conditionals.length) {
                    case 1:
                        radius = parseStringToInt(conditionals[0], 1);
                        break;
                    case 2:
                        radius = parseStringToInt(conditionals[0], 1);
                        mobId = parseStringToInt(conditionals[1], -1);
                        break;
                    default:
                        break;
                }

                // Create and return a new TeleportAction object.
                return new TeleportAction(target, sign.getLocation(), radius, mobId);

            }

            return null;
        });
        put("kill", (Sign sign) -> {
            // Get the first corner of the region from the third line of the sign.
            String[] corner1Components = ((TextComponent) sign.line(2)).content().split(";");
            Location corner1 = null;

            // Parse it into a location object.
            if (corner1Components.length >= 3) {
                if (valueIsNumeric(corner1Components[0]) && valueIsNumeric(corner1Components[1]) && valueIsNumeric(corner1Components[2])) {
                    corner1 = new Location(sign.getWorld(), Double.parseDouble(corner1Components[0]), Double.parseDouble(corner1Components[1]), Double.parseDouble(corner1Components[2]));
                }
            }

            // Check if the first corner was a valid location.
            if (corner1 != null) {

                // Get the second corner of the region from the fourth line of the sign.
                String[] corner2Components = ((TextComponent) sign.line(3)).content().split(";");
                Location corner2 = null;

                if (corner2Components.length >= 3) {
                    if (valueIsNumeric(corner2Components[0]) && valueIsNumeric(corner2Components[1]) && valueIsNumeric(corner2Components[2])) {
                        corner2 = new Location(sign.getWorld(), Double.parseDouble(corner2Components[0]), Double.parseDouble(corner2Components[1]), Double.parseDouble(corner2Components[2]));
                    }
                }

                // Check if the second corner was a valid location.
                if (corner2 != null) {

                    // Create and return a new "KillPlayerAction" object.
                    return new KillPlayerAction(corner1, corner2);

                }

            }

            return null;
        });
        put("experience", (Sign sign) -> {
            // Get the amount and type (level / points) from the third line.
            String[] amountArgs = ((TextComponent) sign.line(2)).content().split(";");
            int amount = 0;
            boolean isLevel = false;

            switch(amountArgs.length) {
                case 1:
                    amount = parseStringToInt(amountArgs[0], 0);
                    isLevel = false;
                    break;
                case 2:
                    amount = parseStringToInt(amountArgs[0], 0);
                    isLevel = amountArgs[1].equalsIgnoreCase("levels");
                    break;
                default:
                    break;
            }

            // Get the mob id and possible radius from the fourth line.
            String[] dataArg = ((TextComponent) sign.line(3)).content().split(";");
            int mobId = -1, radius = 0;

            switch(dataArg.length) {
                case 1:
                    mobId = parseStringToInt(dataArg[0], -1);
                    break;
                case 2:
                    mobId = parseStringToInt(dataArg[0], -1);
                    radius = parseStringToInt(dataArg[1], 0);
                    break;
                default:
                    break;
            }

            // If the mob id is not < 0, create and return the new ExperienceAction object. Otherwise, return null.
            if (mobId >= 0)
                return new ExperienceAction(sign.getLocation(), radius, amount, mobId, isLevel);
            else return null;
        });
        put("final", (Sign sign) -> {
            // Get the mob id from the third line of the sign.
            String idString = ((TextComponent) sign.line(2)).content();
            int mobId = parseStringToInt(idString, -1);

            // Get the instance from the world that the sign is in.
            InstantiatedWorld instance = null;

            final int index = InstancedWorldsManager.getPlayerInstanceIndex(sign.getWorld().getName());
            if (index > -1) {
                instance = InstancedWorldsManager.instances.get(index);
            }

            // Create and return the new InstanceFinalAction object.
            if (mobId > -1 && instance != null) {
                return new InstanceFinalAction(instance, mobId);
            }

            return null;
        });
    }};

    // Function definitions
    /*
        Helper function which will return true or false regarding whether the
        provided String value can be converted to an integer.
    */
    public static boolean valueIsNumeric(String value) {
        boolean numeric;

        try {
            Integer.parseInt(value);
            numeric = true;
        } catch(NumberFormatException e) {
            numeric = false;
        }

        return numeric;
    }

    /*
        Helper function which will attempt to convert the string to a double,
        returning the new integer value if successful, or the provided fallback
        if not possible.
    */
    public static double parseStringToDouble(String value, double fallback) {
        double converted;

        try {
            converted = Double.parseDouble(value);
        } catch(NumberFormatException e) {
            converted = fallback;
        }

        return converted;
    }

    /*
        Helper function which will attempt to convert the string to an integer,
        returning the new integer value if successful, or the provided fallback
        if not possible.
    */
    public static int parseStringToInt(String value, int fallback) {
        int converted;

        try {
            converted = Integer.parseInt(value);
        } catch(NumberFormatException e) {
            converted = fallback;
        }

        return converted;
    }

    /*
        Receives a "String" plugin name, and the function returns a boolean indicating if
        the server has the provided plugin enabled.
    */
    public static boolean isPluginEnabled(String name) {
        return Bukkit.getServer().getPluginManager().isPluginEnabled(name);
    }

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
        Pass a "String" type template id value, which the function will use to check if a
        template world exists with that id.
    */
    public static boolean templateExistsById(String id) {
        boolean exists = false;

        for (int i = 0; i < InstancedWorldsManager.templates.size() && !exists; i++) {
            if (InstancedWorldsManager.templates.get(i).getId().equalsIgnoreCase(id))
                exists = true;
        }

        return exists;
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
        if the provided user is a member of one of them.
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
        Pass a "String" id value for the template, which the function will use to find the
        template in the "templates" list and return its name value.
    */
    public static String getTemplateInstanceNameById(String id) {
        String instanceName = "<Instance Name Not Found>";

        final int index = InstancedWorldsManager.getTemplateIndexById(id);
        if (index > -1) {
            instanceName = InstancedWorldsManager.templates.get(index).getName();
        }

        return instanceName;
    }

    /*
        Pass a "String" id and a "String" name, and the function will use them to create a
        new InstanceTemplate object, store it into the templates list, as well as save the
        relevant data into the config file.
    */
    public static boolean registerTemplateWorld(String id, String name) {
        boolean success = false;

        // If the id isn't prefaced by "template_", update it.
        if (!id.startsWith("template_"))
            id = String.format("template_%s", id);

        // Check if a template already exists with that id or name.
        if (!InstancedWorldsManager.templateExistsWithIdOrName(id, name)) {

            // Save the data into the config file.
            InstancedWorldsManager.saveConfigValue(String.format("templates.%s.name", id), name);

            // Create a new InstanceTemplate object.
            InstanceTemplate template = new InstanceTemplate(id, name);

            // Store the new InstanceTemplate object into the "templates" object.
            InstancedWorldsManager.templates.add(template);

            // Mark the flag as true.
            success = true;

        }

        return success;
    }

    /*
        Pass a "InstanceTemplate" object which the function will store into the templates
        list.
    */
    public static boolean registerTemplateWorld(InstanceTemplate template) {
        boolean success = false;

        if (!InstancedWorldsManager.templateExistsWithIdOrName(template.getId(), template.getName())) {

            // Store the InstanceTemplate object into the templates list.
            InstancedWorldsManager.templates.add(template);

            // Update the flag variable.
            success = true;

        }

        return success;
    }

    /*
        Pass a "String" id and "String" name, which the function will use to verify if there is
        an existing template with either a matching id or a matching name.
    */
    public static boolean templateExistsWithIdOrName(String id, String name) {
        boolean exists = false;

        for (InstanceTemplate template : InstancedWorldsManager.templates) {
            if (template.getId().equalsIgnoreCase(id) || template.getName().equalsIgnoreCase(name))
                exists = true;
        }

        return exists;
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
        return InstancedWorldsManager.templates.stream().map(InstanceTemplate::getId).collect(Collectors.toList());
    }

    /*
        Gets the index of a template based on the provided "String" id value.
    */
    public static int getTemplateIndexById(String id) {
        int index = -1;
        boolean found = false;

        for (int i = 0; i < InstancedWorldsManager.templates.size() && !found; i++) {
            if (InstancedWorldsManager.templates.get(i).getId().equalsIgnoreCase(id)) {
                index = i;
                found = true;
            }
        }

        return index;
    }

    /*
        Pass the name of the world to the function, which it uses
        as it iterates through the entire "templates" list comparing the passed name,
        with the prefix added, and returning either null (if no match was found) or
        the "World" object for the target template world.
    */
    public static World getTemplateWorldById(String id) {
        World templateWorld = null;

        // Find the template index by its id.
        final int index = InstancedWorldsManager.getTemplateIndexById(id);
        if (index > -1) {
            templateWorld = InstancedWorldsManager.templates.get(index).getTemplateWorld();
        }

        return templateWorld;
    }

    /*
        Receives a SummoningInvite object and stores it in the list.
    */
    public static void registerSummoningInvite(SummoningInvite invite) {
        InstancedWorldsManager.summoningInvites.add(invite);
    }

    /*
        Receives a player UUID, and uses it to check if the user has any active summon
        invites. If so, returns true. If not, returns false and removes any timedout
        invites from the list.
    */
    public static boolean playerHasSummoningInvite(UUID player) {
        List<Integer> indexes = new ArrayList<>();
        boolean exists = false;

        for (int i = 0; i < InstancedWorldsManager.summoningInvites.size() && !exists; i++) {

            if (InstancedWorldsManager.summoningInvites.get(i).playerIsRecipient(player)) {
                if (!InstancedWorldsManager.summoningInvites.get(i).isTimedout()) {
                    exists = true;
                } else {
                    indexes.add(i);
                }
            }

        }

        for (int index : indexes) InstancedWorldsManager.summoningInvites.remove(index);

        return exists;
    }

    /*
        Receives a player UUID, and uses it to find and return the index of any
        active summon invites that belong to the player.

        **This function does NOT check the timeout status of the invites.
    */
    public static int getPlayerSummoningInviteIndex(UUID player) {
        int index = -1;

        for (int i = 0; i < InstancedWorldsManager.summoningInvites.size() && index < 0; i++) {
            if (InstancedWorldsManager.summoningInvites.get(i).playerIsRecipient(player)) {
                index = i;
            }
        }

        return index;
    }

    /*
        Receives a new SummoningStone object and stores it in the summoningStones list.
    */
    public static void registerSummoningStone(SummoningStone stone) {
        InstancedWorldsManager.summoningStones.add(stone);
    }

    /*
        Receives a readable id String value and returns a boolean indicating if a summoning
        stone with that id already exists or not.
    */
    public static boolean summoningStoneExistsWithReadableId(String id) {
        boolean exists = false;

        for (SummoningStone stone : InstancedWorldsManager.summoningStones) {
            if (stone.getReadableId().equalsIgnoreCase(id)) {
                exists = true;
            }
        }

        return exists;
    }

    /*
        Receives a readable id String value and returns a boolean indicating if a summoning
        stone with that id already exists or not.
    */
    public static int getSummoningStoneIndexByReadableId(String id) {
        int index = -1;

        for (int i = 0; i < InstancedWorldsManager.summoningStones.size() && index < 0; i++) {
            if (InstancedWorldsManager.summoningStones.get(i).getReadableId().equalsIgnoreCase(id))
                index = i;
        }

        return index;
    }

    /*
        Receives a location and returns the first summoning stone index within the configurable
        radius, if any.
    */
    public static int getSummoningStoneIndexInRange(Location location) {
        int index = -1;

        for (int i = 0; i < InstancedWorldsManager.summoningStones.size() && index < 0; i++) {
            if (InstancedWorldsManager.summoningStones.get(i).getDistance(location) <= InstancedWorldsManager.summonStoneUseRange)
                index = i;
        }

        return index;
    }

}
