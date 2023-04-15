package me.shershnyaga.air;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import me.shershnyaga.air.commands.AirInfinite;
import me.shershnyaga.air.commands.TabPluginHelp;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class AirPlugin extends JavaPlugin implements Listener {

    private static final String boosterTagPrefix = "air:balloon_consumption_booster=";
    private static final BlockData lodestoneData = Material.LODESTONE.createBlockData();
    private final HashMap<UUID, BossBar> bossbars = new HashMap<>();
    private final NamespacedKey balloonAirKey = new NamespacedKey(this, "balloon_air");
    private final NamespacedKey balloonMaxAirKey = new NamespacedKey(this, "balloon_max_air");
    private final NamespacedKey balloonConsumptionRateBoostKey = new NamespacedKey(this, "balloon_consumption_rate_boost");
    private final NamespacedKey balloonConsumptionRateBoosterKey = new NamespacedKey(this, "balloon_consumption_rate_booster");
    private final NamespacedKey particleIdTag = new NamespacedKey(this, "particle_id");
    private final Set<Location> lodestones = new HashSet<>();
    private String bossbarTitleFormat;
    private String bossbarNoAirTitleFormat;
    private String bossbarRegenerationTitleFormat;
    private String bossbarInfiniteAirFormat;
    private BarColor bossbarColor;
    private BarStyle bossbarStyle;
    private boolean bossbarDarkenSky;
    private boolean bossbarFog;
    private ArrayList<UUID> consumingAir = new ArrayList<>();
    private float defaultConsumptionRate;
    private float consumptionRateDamageMultiplier;
    private NamespacedKey balloonWearSoundKey;
    private NamespacedKey arrowHitSoundKey;
    private Particle balloonWearParticle;
    private int balloonWearParticleCount;
    private Particle arrowHitParticle;
    private int arrowHitParticleCount;
    private String arrowHitTitle;
    private String arrowHitSubTitle;
    private int arrowHitTitleFadeIn;
    private int arrowHitTitleStay;
    private int arrowHitTitleFadeOut;
    private int noAirDamage;
    private int balloonRegenerationRate;
    private boolean balloonRegenerationLeakage;
    private int lodestoneDistance;
    private Particle lodestoneParticle;
    private int lodestoneParticleCount;
    private int lodestoneParticleFrequency;
    private Map<String, Integer> balloonCMD;

    private static AirPlugin plugin;
    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        reloadChunks();

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().scheduleSyncRepeatingTask(this, this::updateDimensions, 1, 3);
        getServer().getScheduler().scheduleSyncRepeatingTask(this, this::updateAir, 1, 20);
        getServer().getScheduler().scheduleSyncRepeatingTask(this, this::showLodestoneParticles, 1, lodestoneParticleFrequency);
        getServer().getScheduler().scheduleSyncRepeatingTask(this, this::removeRemovedLodestones, 1, 20);

        Objects.requireNonNull(getServer().getPluginCommand("air")).setExecutor(new AirInfinite());
        Objects.requireNonNull(getServer().getPluginCommand("air")).setTabCompleter(new TabPluginHelp());

        plugin = this;
    }

    @Override
    public void onDisable() {
        bossbars.forEach((uuid, bossbar) -> bossbar.removeAll());
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        bossbarTitleFormat = ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(getConfig().getString("bossbar.title")));
        bossbarNoAirTitleFormat = ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(getConfig().getString("bossbar.title no air")));
        bossbarRegenerationTitleFormat = ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(getConfig().getString("bossbar.regeneration title")));
        bossbarInfiniteAirFormat = ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(getConfig().getString("bossbar.infinite air title")));
        bossbarColor = BarColor.valueOf(getConfig().getString("bossbar.color"));
        bossbarStyle = BarStyle.valueOf(getConfig().getString("bossbar.style"));
        bossbarDarkenSky = getConfig().getBoolean("bossbar.darken sky");
        bossbarFog = getConfig().getBoolean("bossbar.fog");
        defaultConsumptionRate = (float) getConfig().getDouble("consumption rate.default");
        consumptionRateDamageMultiplier = (float) getConfig().getDouble("consumption rate.damage multiplier");
        balloonWearSoundKey = NamespacedKey.fromString(Objects.requireNonNull(getConfig().getString("effects.wear sound")), this);
        arrowHitSoundKey = NamespacedKey.fromString(Objects.requireNonNull(getConfig().getString("effects.arrow hit")), this);
        balloonWearParticle = Particle.valueOf(Objects.requireNonNull(getConfig().getString("effects.wear particle")));
        balloonWearParticleCount = getConfig().getInt("effects.wear particle count");
        arrowHitParticle = Particle.valueOf(Objects.requireNonNull(getConfig().getString("effects.arrow hit particle")));
        arrowHitParticleCount = getConfig().getInt("effects.arrow hit particle count");
        arrowHitTitle = ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(getConfig().getString("effects.arrow hit title")));
        arrowHitSubTitle = ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(getConfig().getString("effects.arrow hit subtitle")));
        arrowHitTitleFadeIn = getConfig().getInt("effects.arrow hit title fade in");
        arrowHitTitleStay = getConfig().getInt("effects.arrow hit title stay");
        arrowHitTitleFadeOut = getConfig().getInt("effects.arrow hit title fade out");
        noAirDamage = getConfig().getInt("no air damage");
        balloonRegenerationRate = getConfig().getInt("consumption rate.regeneration");
        balloonRegenerationLeakage = getConfig().getBoolean("consumption rate.regeneration leakage");
        lodestoneDistance = getConfig().getInt("lodestone distance");
        lodestoneParticle = Particle.valueOf(Objects.requireNonNull(getConfig().getString("effects.lodestone particle")));
        lodestoneParticleCount = getConfig().getInt("effects.lodestone particle count");
        lodestoneParticleFrequency = getConfig().getInt("effects.lodestone particle frequency");
        balloonCMD = (Map<String, Integer>) (Object) getConfig().getConfigurationSection("balloon custom model data").getValues(false);
    }

    public void updateDimensions() {
        consumingAir = new ArrayList<>(consumingAir.size());
        for (Player player : getServer().getOnlinePlayers()) {
            if (shouldConsumeAir(player)) {
                consumingAir.add(player.getUniqueId());
            }
        }
    }

    public boolean shouldConsumeAir(Player player) {
        return player.getWorld().getEnvironment() == World.Environment.THE_END
                && lodestones.stream().noneMatch(l -> l.distance(player.getLocation()) <= lodestoneDistance);
    }

    public void updateAir() {
        for (Player player : getServer().getOnlinePlayers()) {
            updateAir(player);
        }
    }

    public void updateAir(Player player) {
        var air = getAir(player);
        var maxAir = getMaxAir(player);
        if (consumingAir.contains(player.getUniqueId())) {
            createBossbarIfNeeded(player);
            if (air == 0 || !setAir(player, air - getAirConsumptionRate(player))) {
                player.damage(noAirDamage);
            }
        } else {
            if (air != maxAir) {
                createBossbarIfNeeded(player);
                setAir(player,
                        Math.min(maxAir, air +
                                balloonRegenerationRate -
                                (balloonRegenerationLeakage ? getBalloonConsumptionRateBoost(player) : 0)));
            }
        }
        if (!consumingAir.contains(player.getUniqueId()) && getAir(player) == getMaxAir(player)) {
            removeBossbar(player);
        }
    }

    public void createBossbarIfNeeded(Player player) {
        if (bossbars.containsKey(player.getUniqueId())) return;

        BarFlag[] flags;
        if (bossbarDarkenSky && bossbarFog) flags = new BarFlag[]{BarFlag.DARKEN_SKY, BarFlag.CREATE_FOG};
        else if (bossbarDarkenSky) flags = new BarFlag[]{BarFlag.DARKEN_SKY};
        else if (bossbarFog) flags = new BarFlag[]{BarFlag.CREATE_FOG};
        else flags = new BarFlag[0];

        var bossbar = getServer().createBossBar(getBossbarTitle(player),
                bossbarColor,
                bossbarStyle,
                flags);
        bossbar.setProgress(getBalloonRatio(player));

        bossbar.addPlayer(player);

        bossbars.put(player.getUniqueId(), bossbar);
    }

    public void removeBossbar(Player player) {
        if (bossbars.containsKey(player.getUniqueId())) {
            bossbars.remove(player.getUniqueId()).removeAll();
        }
    }

    public float getAir(LivingEntity entity) {

        if (entity.getPersistentDataContainer().has(new NamespacedKey(AirPlugin.getPlugin(), "AirInf"), PersistentDataType.INTEGER)) return 1;
        return getValue(entity, balloonAirKey);
    }

    public float getMaxAir(LivingEntity entity) {

        if (entity.getPersistentDataContainer().has(new NamespacedKey(AirPlugin.getPlugin(), "AirInf"), PersistentDataType.INTEGER)) return 1;
        return getValue(entity, balloonMaxAirKey);
    }

    private float getValue(LivingEntity entity, NamespacedKey key) {
        ItemStack chestplate = Objects.requireNonNull(entity.getEquipment()).getChestplate();
        return getItemValue(chestplate, key);
    }

    private float getItemValue(ItemStack item, NamespacedKey key) {
        if (item != null && item.getItemMeta() != null && item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.FLOAT)) {
            return Objects.requireNonNull(Objects.requireNonNull(item.getItemMeta()).getPersistentDataContainer().get(key, PersistentDataType.FLOAT));
        } else {
            return 0;
        }
    }

    public boolean setAir(Player player, float amount) {
        if (amount < 0) amount = 0;
        if (player.getPersistentDataContainer().has(new NamespacedKey(AirPlugin.getPlugin(), "AirInf"), PersistentDataType.INTEGER)) return true;
        return setValue(player, amount, balloonAirKey);
    }

    public boolean setMaxAir(Player player, float amount) {
        if (amount < 0) amount = Float.MIN_VALUE;
        if (player.getPersistentDataContainer().has(new NamespacedKey(AirPlugin.getPlugin(), "AirInf"), PersistentDataType.INTEGER)) return true;
        return setValue(player, amount, balloonMaxAirKey);
    }

    public void updateBossbar(Player player) {
        if (player.getPersistentDataContainer().has(new NamespacedKey(AirPlugin.getPlugin(), "AirInf"), PersistentDataType.INTEGER)) {
            bossbars.get(player.getUniqueId()).setProgress(getBalloonRatio(player));
            bossbars.get(player.getUniqueId()).setTitle(bossbarInfiniteAirFormat);
        } else {
            bossbars.get(player.getUniqueId()).setProgress(getBalloonRatio(player));
            bossbars.get(player.getUniqueId()).setTitle(getBossbarTitle(player));
        }
    }

    private boolean setValue(LivingEntity entity, float amount, NamespacedKey key) {
        var equipment = entity.getEquipment();
        if (equipment == null) return false;
        ItemStack chestplate = equipment.getChestplate();
        if (chestplate == null) return false;
        setItemValue(chestplate, amount, key);
        return true;
    }

    private void setItemValue(ItemStack item, float amount, NamespacedKey key) {
        var meta = Objects.requireNonNull(item.getItemMeta());
        meta.getPersistentDataContainer().set(key, PersistentDataType.FLOAT, amount);
        item.setItemMeta(meta);
    }

    public float getBalloonRatio(Player player) {
        var air = getAir(player);
        var max = getMaxAir(player);
        return max == 0 ? 0 : air / max;
    }

    public String getBossbarTitle(Player player) {
        var air = getAir(player);
        var max = getMaxAir(player);
        if (player.getPersistentDataContainer().has(new NamespacedKey(AirPlugin.getPlugin(), "AirInf"), PersistentDataType.INTEGER)){
            return bossbarInfiniteAirFormat;
        } else if (air == 0) {
            return bossbarNoAirTitleFormat
                    .replaceAll("<air>", "0")
                    .replaceAll("<max air>", String.valueOf((int) max));
        } else if (consumingAir.contains(player.getUniqueId())) {
            return bossbarTitleFormat
                    .replaceAll("<air>", String.valueOf((int) air))
                    .replaceAll("<max air>", String.valueOf((int) max));
        } else {
            return bossbarRegenerationTitleFormat
                    .replaceAll("<air>", String.valueOf((int) air))
                    .replaceAll("<max air>", String.valueOf((int) max));
        }
    }

    public float getAirConsumptionRate(Player player) {
        return defaultConsumptionRate +
                getHealthConsumptionRate(player) +
                getBalloonConsumptionRateBoost(player);
    }

    public float getHealthConsumptionRate(Player player) {
        return (1.0f - (float) player.getHealth() /
                (float) Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getValue()) *
                consumptionRateDamageMultiplier;
    }

    public float getBalloonConsumptionRateBoost(Player player) {
        return getValue(player, balloonConsumptionRateBoostKey);
    }

    @EventHandler
    public void onArrowLaunch(EntityShootBowEvent event) {
        var booster = getItemValue(event.getConsumable(), balloonConsumptionRateBoosterKey);
        if (booster != 0) {
            event.getProjectile().addScoreboardTag(boosterTagPrefix + booster);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onArrowHit(ProjectileHitEvent event) {
        if (event.getHitEntity() != null && event.getHitEntity() instanceof LivingEntity entity) {
            event.getEntity().getScoreboardTags().stream().filter(tag -> tag.startsWith(boosterTagPrefix)).findAny().ifPresent(tag -> {
                var boost = Float.parseFloat(tag.substring(boosterTagPrefix.length()));
                damageEntityBalloon(entity, boost);
            });
        }
    }

    public void damageEntityBalloon(LivingEntity entity, float consumptionBoost) {
        if (setValue(entity, getValue(entity, balloonConsumptionRateBoostKey) + consumptionBoost, balloonConsumptionRateBoostKey)) {
            entity.playSound(Sound.sound(arrowHitSoundKey, Sound.Source.PLAYER, 1, 1));
            entity.getWorld().spawnParticle(arrowHitParticle, entity.getLocation().clone().add(0, 1, 0), arrowHitParticleCount);
            entity.showTitle(Title.title(Component.text(arrowHitTitle),
                    Component.text(arrowHitSubTitle),
                    Title.Times.of(Ticks.duration(arrowHitTitleFadeIn),
                            Ticks.duration(arrowHitTitleStay),
                            Ticks.duration(arrowHitTitleFadeOut))));
            updateCustomModelData(entity);
        }
    }
    
    public void updateCustomModelData(LivingEntity entity) {
        var equipment = entity.getEquipment();
        if (equipment == null) return;
        ItemStack chestplate = equipment.getChestplate();
        if (chestplate == null) return;
        var meta = Objects.requireNonNull(chestplate.getItemMeta());
        meta.setCustomModelData(balloonCMD.getOrDefault(String.valueOf((int) getMaxAir(entity)), balloonCMD.get("default")) +
                Math.min(balloonCMD.get("max damage"), (int) getValue(entity, balloonConsumptionRateBoostKey)));
        chestplate.setItemMeta(meta);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onArmorChange(PlayerArmorChangeEvent event) {
        if (getItemValue(event.getNewItem(), balloonMaxAirKey) != 0 || getItemValue(event.getOldItem(), balloonMaxAirKey) != 0) {
            if (getValue(event.getPlayer(), particleIdTag) == 0) {
                setValue(event.getPlayer(), new Random().nextFloat(), particleIdTag);
            }
            if (getItemValue(event.getOldItem(), particleIdTag) != getItemValue(event.getNewItem(), particleIdTag)) {
                updateAir(event.getPlayer());
                event.getPlayer().playSound(Sound.sound(balloonWearSoundKey, Sound.Source.PLAYER, 1, 1));
                event.getPlayer().getWorld().spawnParticle(balloonWearParticle, event.getPlayer().getLocation().clone().add(0, 1, 0), balloonWearParticleCount);
                updateCustomModelData(event.getPlayer());
            }
            if (bossbars.containsKey(event.getPlayer().getUniqueId())) {
                updateBossbar(event.getPlayer());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        loadChunk(event.getChunk());
    }

    private void loadChunk(Chunk chunk) {
        if (chunk.getWorld().getEnvironment() == World.Environment.THE_END) {
            if (chunk.contains(lodestoneData)) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = chunk.getWorld().getMinHeight(); y < chunk.getWorld().getMaxHeight(); y++) {
                            if (chunk.getBlock(x, y, z).getBlockData().equals(lodestoneData)) {
                                lodestones.add(chunk.getBlock(x, y, z).getLocation());
                            }
                        }
                    }
                }
            }
        }
    }

    private void unloadChunk(Chunk chunk) {
        if (chunk.getWorld().getEnvironment() == World.Environment.THE_END) {
            lodestones.removeIf(l -> l.getChunk().equals(chunk));
        }
    }

    private void reloadChunks() {
        for (World world : getServer().getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                unloadChunk(chunk);
                loadChunk(chunk);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        unloadChunk(event.getChunk());
    }

    public void showLodestoneParticles() {
        for (Location lodestone : lodestones) {
            int i = 0;
            while (i < 5) {
                i++;
                var random = new Random();
                var theta = random.nextDouble(Math.PI * 2);
                var v = random.nextDouble();
                var phi = Math.acos(2 * v - 1);
                var r = Math.pow(random.nextDouble(), (float) 1 / 3);
                var x = lodestoneDistance * r * Math.sin(phi) * Math.cos(theta) + 0.5;
                var y = lodestoneDistance * r * Math.sin(phi) * Math.sin(theta) + 0.5 + 1.62;
                var z = lodestoneDistance * r * Math.cos(phi) + 0.5;
                var particleLocation = lodestone.clone().add(x, y, z);
                if (lodestone.getWorld().getBlockAt(particleLocation).isEmpty()) {
                    lodestone.getWorld().spawnParticle(lodestoneParticle,
                            particleLocation,
                            lodestoneParticleCount);
                    break;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLodestonePlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getBlockData().equals(lodestoneData)) {
            lodestones.add(event.getBlockPlaced().getLocation());
        }
    }

    public void removeRemovedLodestones() {
        lodestones.removeIf(lodestone -> !lodestone.isChunkLoaded() || !lodestone.getBlock().getBlockData().equals(lodestoneData));
    }

    public static AirPlugin getPlugin() {
        return plugin;
    }

}
