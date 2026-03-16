package com.github.cinnaio.natureEngine.command;

import com.github.cinnaio.natureEngine.api.SeasonAPI;
import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonType;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SeasonCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("只能玩家执行该命令。");
            return true;
        }
        Player player = (Player) sender;
        World world = player.getWorld();

        SeasonType type = SeasonAPI.getCurrentSeason(world);
        double progress = SeasonAPI.getSeasonProgress(world);

        sender.sendMessage("当前季节: " + type + " 进度: " + String.format("%.2f", progress * 100) + "%");
        return true;
    }
}

