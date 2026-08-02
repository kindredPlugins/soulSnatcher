package at.gaderman.soulSnatcher.commands;

import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.items.SoulLanternManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SoulLanternCommand implements CommandExecutor, TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        if(!(sender instanceof Player player)){
            sender.sendMessage(Component.text("You need to be a player to perform this command!"));
            return true;
        }

        if(SoulType.getCarriedSouls(player).isEmpty()){
            player.sendMessage(Component.text("You do not own any souls, so no soul lantern was added", NamedTextColor.GRAY));
            player.playSound(player, Sound.ENTITY_ARROW_SHOOT, 1f, 2f);
            SoulLanternManager.updateActiveLanterns(player);
            return true;
        }

        player.sendMessage(Component.text("SoulLantern has been updated!", NamedTextColor.GRAY));
        player.playSound(player, Sound.BLOCK_PUMPKIN_CARVE, 1f, 1f);
        SoulLanternManager.updateActiveLanterns(player);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        return List.of();
    }
}
