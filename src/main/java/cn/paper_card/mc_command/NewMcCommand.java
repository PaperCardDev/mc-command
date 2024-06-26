package cn.paper_card.mc_command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("unused")
public abstract class NewMcCommand implements CommandExecutor, TabCompleter {

    protected class Sender extends MessageSender {
        public Sender(@NotNull CommandSender sender) {
            super(sender);
        }

        @Override
        public void appendPrefix(TextComponent.@NotNull Builder text) {
            NewMcCommand.this.appendPrefix(text);
        }
    }

    protected final @NotNull String label;

    protected NewMcCommand(@NotNull String label) {
        this.label = label;
    }

    public @NotNull String getLabel() {
        return this.label;
    }

    public void register(@NotNull JavaPlugin plugin) {
        final PluginCommand pc = plugin.getCommand(this.label);
        assert pc != null;
        pc.setExecutor(this);
        pc.setTabCompleter(this);
    }

    protected abstract boolean canExecute(@NotNull CommandSender commandSender);

    protected void appendPrefix(@NotNull TextComponent.Builder text) {
    }

    protected @NotNull Permission addSubPermission(@NotNull PluginManager pluginManager, @NotNull Permission permParent) {
        final Permission p = new Permission(permParent.getName() + "." + this.getLabel());
        pluginManager.addPermission(p);
        return p;
    }

    public static @NotNull List<String> tabCompleteOfflinePlayerNames(@NotNull String arg, @NotNull Server server, boolean ignoreEmptyArg, @Nullable String tip) {
        final LinkedList<String> list = new LinkedList<>();

        final Runnable runnable = () -> {
            for (OfflinePlayer offlinePlayer : server.getOfflinePlayers()) {
                final String name = offlinePlayer.getName();
                if (name == null) continue;
                if (name.startsWith(arg)) list.add(name);
            }
        };

        if (arg.isEmpty()) {
            if (tip != null) list.add(tip);
            if (!ignoreEmptyArg) runnable.run();
        } else {
            runnable.run();
        }
        return list;
    }

    public static @Nullable OfflinePlayer parseOfflinePlayerName(@NotNull String nameOrUuid, @NotNull Server server) {

        try {
            final UUID uuid = UUID.fromString(nameOrUuid);
            return server.getOfflinePlayer(uuid);
        } catch (IllegalArgumentException ignored) {
        }

        final OfflinePlayer player = server.getOfflinePlayerIfCached(nameOrUuid);
        if (player != null) return player;

        for (OfflinePlayer offlinePlayer : server.getOfflinePlayers()) {
            final String name = offlinePlayer.getName();
            if (nameOrUuid.equals(name)) return offlinePlayer;
        }

        return null;
    }

    public static @NotNull List<String> tabCompleteOnlinePlayerNames(@NotNull String arg, @NotNull String tip, @NotNull Server server) {
        final LinkedList<String> list = new LinkedList<>();
        for (Player onlinePlayer : server.getOnlinePlayers()) {
            final String name = onlinePlayer.getName();
            if (name.startsWith(arg)) list.add(name);
        }
        return list;
    }

    public static @Nullable Player parseOnlinePlayerName(@NotNull String name, @NotNull Server server) {
        for (Player onlinePlayer : server.getOnlinePlayers()) {
            if (onlinePlayer.getName().equals(name)) return onlinePlayer;
        }
        return null;
    }

    public static abstract class HasSub extends NewMcCommand {

        private final @NotNull HashMap<String, NewMcCommand> subCommands = new HashMap<>();

        public HasSub(@NotNull String label) {
            super(label);
        }

        public void addSub(@NotNull NewMcCommand theCommand) {
            final String l = theCommand.getLabel();
            synchronized (this.subCommands) {
                if (this.subCommands.containsKey(l)) throw new RuntimeException("子命令[%s]已经存在！".formatted(l));

                this.subCommands.put(l, theCommand);
            }
        }

        protected boolean onNotFound(@NotNull CommandSender sender, @NotNull String sub) {
            final TextComponent.Builder text = Component.text();
            new Sender(sender).appendPrefix(text);
            text.appendSpace();
            text.append(Component.text("不存在的子命令："));
            text.append(Component.text(sub).decorate(TextDecoration.BOLD));
            text.append(Component.text("，可用的子命令："));

            synchronized (this.subCommands) {
                int c = 0;

                for (final String l : this.subCommands.keySet()) {
                    final NewMcCommand command = this.subCommands.get(l);
                    if (!command.canExecute(sender)) continue;

                    if (c > 0) text.append(Component.text("、"));
                    text.append(Component.text(l).color(NamedTextColor.GREEN));

                    ++c;
                }
            }

            sender.sendMessage(text.build().color(MessageSender.COLOR_ERROR));

            return true;
        }

        protected boolean onThisCommand(@NotNull CommandSender sender) {
            final TextComponent.Builder text = Component.text();
            new Sender(sender).appendPrefix(text);
            text.appendSpace();
            text.append(Component.text("可用的子命令："));

            synchronized (this.subCommands) {
                int c = 0;

                for (final String l : this.subCommands.keySet()) {
                    final NewMcCommand command = this.subCommands.get(l);
                    if (!command.canExecute(sender)) continue;

                    if (c > 0) text.append(Component.text("、"));
                    text.append(Component.text(l).color(NamedTextColor.GREEN));

                    ++c;
                }
            }

            sender.sendMessage(text.build().color(MessageSender.COLOR_INFO));

            return true;
        }

        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            if (args.length > 0) {
                final String cmd = args[0];
                final NewMcCommand tc = this.subCommands.get(cmd);
                if (tc != null) {
                    if (tc.canExecute(sender)) {
                        final String[] subArgs = new String[args.length - 1];
                        System.arraycopy(args, 1, subArgs, 0, subArgs.length);
                        return tc.onCommand(sender, command, label, subArgs);
                    } else {
                        new Sender(sender).error("您没有权限执行此命令！");
                        return true;
                    }
                } else {
                    return this.onNotFound(sender, cmd);
                }
            } else {
                return this.onThisCommand(sender);
            }
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            if (args.length < 1) return null;

            if (args.length == 1) {

                final LinkedList<String> list = new LinkedList<>();

                for (final String subCmdLabel : this.subCommands.keySet()) {
                    final NewMcCommand theMcCommand = this.subCommands.get(subCmdLabel);
                    if (theMcCommand == null) continue;

                    if (!theMcCommand.getLabel().startsWith(args[0])) continue;

                    if (!theMcCommand.canExecute(sender)) continue;

                    list.add(subCmdLabel);
                }

                return list;

            } else {
                final String string = args[0];

                final NewMcCommand theCommand = this.subCommands.get(string);
                if (theCommand == null) return null;

                final String[] subArgs = new String[args.length - 1];
                System.arraycopy(args, 1, subArgs, 0, subArgs.length);

                return theCommand.onTabComplete(sender, command, label, subArgs);
            }
        }
    }
}