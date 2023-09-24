package cn.paper_card.mc_command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("unused")
public abstract class TheMcCommand implements CommandExecutor, TabCompleter {

    private final @NotNull String label;

    protected TheMcCommand(@NotNull String label) {
        this.label = label;
    }

    public @NotNull String getLabel() {
        return this.label;
    }

    protected abstract boolean canNotExecute(@NotNull CommandSender commandSender);

    @Override
    public abstract boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings);

    @Override
    public abstract @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings);

    public static abstract class HasSub extends TheMcCommand {

        private final HashMap<String, TheMcCommand> subCommands = new HashMap<>();

        public HasSub(@NotNull String label) {
            super(label);
        }

        @Override
        protected abstract boolean canNotExecute(@NotNull CommandSender commandSender);

        public void addSubCommand(@NotNull TheMcCommand theCommand) {
            final String l = theCommand.getLabel();
            synchronized (this.subCommands) {
                if (this.subCommands.containsKey(l))
                    throw new RuntimeException("子命令[%s]已经存在！".formatted(l));
                this.subCommands.put(l, theCommand);
            }
        }

        protected void onNotFound(@NotNull CommandSender sender, @Nullable String sub) {
            sender.sendMessage(Component.text("找不到该子命令：%s".formatted(sub)));
        }

        protected void onThisCommand(@NotNull CommandSender sender) {
            final TextComponent.Builder text = Component.text();
            text.append(Component.text("可用的子命令："));
            synchronized (this.subCommands) {
                for (final String l : this.subCommands.keySet()) {
                    final TheMcCommand command = this.subCommands.get(l);
                    if (command.canNotExecute(sender)) continue;
                    text.append(Component.text(l).color(NamedTextColor.GREEN));
                    text.append(Component.space());
                }
            }
            sender.sendMessage(text.build());
        }

        @Override
        public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
            if (strings.length > 0) {
                final String cmd = strings[0];
                final TheMcCommand tc = subCommands.get(cmd);
                if (tc != null) {
                    if (tc.canNotExecute(commandSender)) return true;
                    final String[] args = new String[strings.length - 1];
                    System.arraycopy(strings, 1, args, 0, args.length);
                    return tc.onCommand(commandSender, command, s, args);
                } else {
                    this.onNotFound(commandSender, cmd);
                }
            } else {
                this.onThisCommand(commandSender);
            }
            return true;
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
            if (strings.length < 1) return null;

            if (strings.length == 1) {

                final LinkedList<String> strings1 = new LinkedList<>();

                for (final String s1 : this.subCommands.keySet()) {
                    final TheMcCommand theMcCommand = this.subCommands.get(s1);
                    if (theMcCommand == null) continue;

                    if (!theMcCommand.getLabel().startsWith(strings[0])) continue;
                    if (theMcCommand.canNotExecute(commandSender)) continue;

                    strings1.add(s1);
                }
                return strings1;

            } else {

                final String string = strings[0];
                final TheMcCommand theCommand = this.subCommands.get(string);
                if (theCommand != null) {
                    final String[] args = new String[strings.length - 1];
                    System.arraycopy(strings, 1, args, 0, args.length);
                    return theCommand.onTabComplete(commandSender, command, s, args);
                }
            }
            return new LinkedList<>();
        }
    }
}
