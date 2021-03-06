package mindustry.plugin;

import arc.math.Mathf;
import arc.util.Log;
import arc.util.Time;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.entities.type.Player;
import mindustry.entities.type.TileEntity;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.maps.Map;
import mindustry.net.Administration;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock;

import static mindustry.Vars.world;
import static mindustry.plugin.Utils.*;

public class MapRules {

    public static CoreBlock.CoreEntity getCore(Team team){
        Tile[][] tiles = world.getTiles();
        for (int x = 0; x < tiles.length; ++x) {
            for(int y = 0; y < tiles[0].length; ++y) {
                if (tiles[x][y] != null && tiles[x][y].entity != null) {
                    TileEntity ent = tiles[x][y].ent();
                    if (ent instanceof CoreBlock.CoreEntity) {
                        if(ent.getTeam() == team){
                            return (CoreBlock.CoreEntity) ent;
                        }
                    }
                }
            }
        }
        return null;
    }


    public static void onMapLoad(){
        Map map = world.getMap();

        // spawn all players quick for the first time
        for(Player p : Vars.playerGroup.all()){
            if(p.dead){
                CoreBlock.CoreEntity ce = getCore(p.getTeam());
                if(ce == null) return;
                p.beginRespawning(ce);
                Timer.schedule(() -> p.onRespawn(ce.tile), Mathf.random(0.1f, 2f));
            }
        }

        Vars.netServer.admins.addActionFilter(action -> {
            Player player = action.player;
            if (player == null) return true;

            String uuid = player.uuid;
            if (uuid == null) return true;

            PlayerData pd = getData(uuid);
            if (pd == null) return true;

            // disable checks for admins
            if (player.isAdmin) return true;

            if (!pd.verified) {
                Call.setHudTextReliable(player.con, "[scarlet]Your IP was flagged as a VPN, please join http://discord.mindustry.io and request manual verification.");
                player.sendMessage("[#7a7a7a]Cannot build while flagged.");
                return false;
            }

            return action.type != Administration.ActionType.rotate;
        });

        // display map description on core tiles for the first minute
        Call.onInfoToast("Playing [accent]" + escapeColorCodes(map.name()) + "[] by[accent] " + map.author(), 20f); // credit map makers

        if(map.description().equals("???unknown???")) return;
        Tile[][] tiles = world.getTiles();
        for (int x = 0; x < tiles.length; ++x) {
            for(int y = 0; y < tiles[0].length; ++y) {
                if (tiles[x][y] != null && tiles[x][y].entity != null) {
                    TileEntity ent = tiles[x][y].ent();
                    if (ent instanceof CoreBlock.CoreEntity) {
                        Call.onLabel(map.description(), 20f, ent.x, ent.y);
                    }
                }
            }
        }
    }

    public static void run(){
        onMapLoad();

        for (java.util.Map.Entry<String, TempPlayerData> entry : ioMain.playerDataGroup.entrySet()) {
            TempPlayerData tdata = entry.getValue();
            if(tdata != null) {
                tdata.spawnedPowerGen = false;
                tdata.spawnedLichPet = false;
                tdata.draugPets.clear();
            }
        }


        Map map = world.getMap();
        switch(map.name()){
            case "Minefield":
                Log.info("[MapRules]: Minefield action trigerred.");
                Call.sendMessage("[accent]Preparing minefield map, please wait.");

                for(int x = 0; x < world.width(); x++){
                    for(int y = 0; y < world.height(); y++){
                        Tile tile = world.tile(x, y);
                        if(tile.block() == Blocks.shockMine){
                            Time.run(Mathf.random(60f * 60f * 10), () -> { // clear out all mines after 10 minutes.
                                if(tile.block() == Blocks.shockMine && map.name().equals("Minefield")){ // check if mine still exists & map is still minefield lol
                                    tile.entity.kill();
                                }
                            });
                        }
                    }
                }
                Call.sendMessage("[accent]Map script loaded. Good luck!");
                break;
            case "test": // feel free to add your own scripts for your custom maps!
                Log.info("Test map loaded.");
                break;

        }
    }
}
