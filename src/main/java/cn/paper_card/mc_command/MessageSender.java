package cn.paper_card.mc_command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class MessageSender {
    public static final @NotNull TextColor COLOR_ERROR = NamedTextColor.RED;
    public static final @NotNull TextColor COLOR_WARNING = NamedTextColor.YELLOW;
    public static final @NotNull TextColor COLOR_INFO = NamedTextColor.GREEN;

    private final @NotNull CommandSender sender;

    public MessageSender(@NotNull CommandSender sender) {
        this.sender = sender;
    }

    public void appendPrefix(@NotNull TextComponent.Builder text) {
    }

    public void info(@NotNull String info) {
        final TextComponent.Builder text = Component.text();
        this.appendPrefix(text);
        text.appendSpace();
        text.append(Component.text(info).color(COLOR_INFO));
        sender.sendMessage(text.build());
    }

    public void warning(@NotNull String warning) {
        final TextComponent.Builder text = Component.text();
        this.appendPrefix(text);
        text.appendSpace();
        text.append(Component.text(warning).color(COLOR_WARNING));
        sender.sendMessage(text.build());
    }

    public void error(@NotNull String error) {
        final TextComponent.Builder text = Component.text();
        this.appendPrefix(text);
        text.appendSpace();
        text.append(Component.text(error).color(COLOR_ERROR));
        sender.sendMessage(text.build());
    }

    public void exception(@NotNull Throwable e) {
        final TextComponent.Builder text = Component.text();
        this.appendPrefix(text);
        text.appendSpace();
        text.append(Component.text("==== 异常信息 ====").color(NamedTextColor.DARK_RED));

        for (Throwable t = e; t != null; t = t.getCause()) {
            text.appendNewline();
            text.append(Component.text(e.toString()).color(COLOR_ERROR));
        }
        sender.sendMessage(text.build());
    }
}
