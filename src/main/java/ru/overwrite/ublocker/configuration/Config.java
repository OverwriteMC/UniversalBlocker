package ru.overwrite.ublocker.configuration;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import lombok.AccessLevel;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.overwrite.ublocker.UniversalBlocker;
import ru.overwrite.ublocker.actions.Action;
import ru.overwrite.ublocker.blockgroups.BlockFactor;
import ru.overwrite.ublocker.blockgroups.BlockType;
import ru.overwrite.ublocker.blockgroups.CommandGroup;
import ru.overwrite.ublocker.blockgroups.SymbolGroup;
import ru.overwrite.ublocker.conditions.Condition;
import ru.overwrite.ublocker.configuration.data.*;
import ru.overwrite.ublocker.listeners.chat.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Getter
public class Config {

    @Getter(AccessLevel.NONE)
    private final UniversalBlocker plugin;

    public Config(UniversalBlocker plugin) {
        this.plugin = plugin;
    }

    private ObjectSet<CommandGroup> commandBlockGroupSet;

    private ObjectSet<SymbolGroup> symbolBlockGroupSet;

    private Set<String> excludedPlayers;

    private CharsSettings chatCharsSettings;
    private CharsSettings bookCharsSettings;
    private CharsSettings signCharsSettings;
    private CharsSettings commandCharsSettings;
    private NumberCheckSettings numberCheckSettings;
    private CaseCheckSettings caseCheckSettings;
    private SameMessagesSettings sameMessagesSettings;
    private BanWordsSettings banWordsSettings;

    public void setupChat(String path) {
        final FileConfiguration chat = getFile(path, "chat.yml");
        final ConfigurationSection settings = chat.getConfigurationSection("chat_settings");
        this.chatCharsSettings = setupCharsSettings(settings.getConfigurationSection("allowed_chat_chars"), ChatFilter.class.getSimpleName());
        this.bookCharsSettings = setupCharsSettings(settings.getConfigurationSection("allowed_book_chars"), BookFilter.class.getSimpleName());
        this.signCharsSettings = setupCharsSettings(settings.getConfigurationSection("allowed_sign_chars"), SignFilter.class.getSimpleName());
        this.commandCharsSettings = setupCharsSettings(settings.getConfigurationSection("allowed_command_chars"), CommandFilter.class.getSimpleName());
        this.numberCheckSettings = setupNumberCheck(settings.getConfigurationSection("numbers_check"));
        this.caseCheckSettings = setupCaseCheck(settings.getConfigurationSection("case_check"));
        this.sameMessagesSettings = setupSameMessages(settings.getConfigurationSection("same_messages"));
        this.banWordsSettings = setupBanWords(settings.getConfigurationSection("ban_words_chat"));
    }

    private CharsSettings setupCharsSettings(ConfigurationSection allowedChars, String listenerKey) {
        if (isNullSection(allowedChars)) {
            return null;
        }
        updateListenerRegistration(allowedChars, listenerKey);
        return createCharsSettings(allowedChars, getChatActions(allowedChars));
    }

    private CharsSettings createCharsSettings(ConfigurationSection section, ObjectList<Action> actions) {
        BlockType mode = BlockType.valueOf(section.getString("mode").toUpperCase());
        IntSet charSet = null;
        Pattern pattern = null;
        switch (mode) {
            case STRING -> charSet = getAllowedChars(section.getString("pattern"));
            case PATTERN -> pattern = Pattern.compile(section.getString("pattern"));
        }
        return new CharsSettings(mode, charSet, pattern, actions);
    }

    private IntSet getAllowedChars(String allowed) {
        IntSet chars = new IntOpenHashSet();
        for (int i = 0, length = allowed.length(); i < length; ) {
            int codePoint = allowed.codePointAt(i);
            chars.add(codePoint);
            i += Character.charCount(codePoint);
        }
        return chars;
    }

    private NumberCheckSettings setupNumberCheck(ConfigurationSection numbersCheck) {
        if (isNullSection(numbersCheck)) {
            return null;
        }
        updateListenerRegistration(numbersCheck, NumbersCheck.class.getSimpleName());

        int maxNumbers = numbersCheck.getInt("maxmsgnumbers", 12);
        boolean strictCheck = numbersCheck.getBoolean("strict");
        boolean stripColor = numbersCheck.getBoolean("strip_color");

        ObjectList<Action> actionList = getChatActions(numbersCheck);

        return new NumberCheckSettings(
                maxNumbers,
                strictCheck,
                stripColor,
                actionList
        );
    }

    private CaseCheckSettings setupCaseCheck(ConfigurationSection caseCheck) {
        if (isNullSection(caseCheck)) {
            return null;
        }
        updateListenerRegistration(caseCheck, CaseCheck.class.getSimpleName());

        int maxUpperCasePercent = caseCheck.getInt("max_uppercase_percent", 70);
        boolean strictCheck = caseCheck.getBoolean("strict");

        ObjectList<Action> actionList = getChatActions(caseCheck);

        return new CaseCheckSettings(
                maxUpperCasePercent,
                strictCheck,
                actionList
        );
    }

    private SameMessagesSettings setupSameMessages(ConfigurationSection sameMessages) {
        if (isNullSection(sameMessages)) {
            return null;
        }
        updateListenerRegistration(sameMessages, SameMessageLimiter.class.getSimpleName());

        int samePercents = sameMessages.getInt("same_percents", 70);
        int maxSameMessage = sameMessages.getInt("max_same_message", 2);
        int minMessageLength = sameMessages.getInt("min_message_length", 3);
        int historySize = sameMessages.getInt("history_size", 10);
        long historyClearAfterQuit = sameMessages.getLong("history_clear_after_quit", 60);
        boolean stripColor = sameMessages.getBoolean("strip_color");

        ObjectList<Action> actionList = getChatActions(sameMessages);

        return new SameMessagesSettings(
                samePercents,
                maxSameMessage,
                minMessageLength,
                historySize,
                historyClearAfterQuit,
                stripColor,
                actionList
        );
    }

    private BanWordsSettings setupBanWords(ConfigurationSection banWords) {
        if (isNullSection(banWords)) {
            return null;
        }
        updateListenerRegistration(banWords, BanWords.class.getSimpleName());

        BlockType mode = BlockType.valueOf(banWords.getString("mode").toUpperCase());
        ObjectSet<String> banWordsString = null;
        ObjectSet<Pattern> banWordsPattern = null;
        List<String> rawBanWordsList = banWords.getStringList("words");
        List<String> banWordsList = new ObjectArrayList<>(rawBanWordsList.size());
        for (String banword : rawBanWordsList) {
            banWordsList.add(banword.toLowerCase());
        }
        switch (mode) {
            case STRING:
                banWordsString = new ObjectOpenHashSet<>(banWordsList);
                break;
            case PATTERN:
                banWordsPattern = new ObjectOpenHashSet<>(banWordsList.size());
                for (String patternString : banWordsList) {
                    Pattern pattern = Pattern.compile(patternString);
                    banWordsPattern.add(pattern);
                }
                break;
        }

        boolean strict = banWords.getBoolean("strict");
        String censorSymbol = String.valueOf(banWords.getString("censor_symbol", "*").charAt(0));
        boolean stripColor = banWords.getBoolean("strip_color");

        ObjectList<Action> actionList = getChatActions(banWords);

        return new BanWordsSettings(
                mode,
                banWordsString,
                banWordsPattern,
                strict,
                censorSymbol,
                stripColor,
                actionList
        );
    }

    private boolean isNullSection(ConfigurationSection section) {
        return section == null;
    }

    private void updateListenerRegistration(ConfigurationSection section, String listenerKey) {
        ChatListener chatListener = plugin.getChatListeners().get(listenerKey);
        boolean shouldBeRegistered = section.getBoolean("enable");
        if (chatListener.isRegistered() != shouldBeRegistered) {
            chatListener.setRegistered(shouldBeRegistered);
        }
    }

    public void setupCommands(String path) {
        final FileConfiguration commands = getFile(path, "commands.yml");
        Set<String> keys = commands.getConfigurationSection("commands").getKeys(false);
        ObjectSet<CommandGroup> commandBlockGroupSetBuilder = new ObjectOpenHashSet<>(keys.size());
        for (String commandsID : keys) {
            final ConfigurationSection section = commands.getConfigurationSection("commands." + commandsID);
            BlockType blockType = BlockType.valueOf(section.getString("mode").toUpperCase());
            boolean blockAliases = section.getBoolean("block_aliases") && blockType.isString();
            boolean whitelistMode = section.getBoolean("whitelist_mode");
            ObjectList<Condition> conditionList = getConditionList(section.getStringList("conditions"));
            ObjectList<Action> actionList = getActionList(section.getStringList("actions"));
            commandBlockGroupSetBuilder.add(
                    new CommandGroup(
                            commandsID,
                            blockType,
                            blockAliases,
                            whitelistMode,
                            section.getStringList("commands"),
                            conditionList,
                            actionList
                    )
            );
        }
        this.commandBlockGroupSet = commandBlockGroupSetBuilder;
    }

    public void setupSymbols(String path) {
        final FileConfiguration symbols = getFile(path, "symbols.yml");
        Set<String> keys = symbols.getConfigurationSection("symbols").getKeys(false);
        ObjectSet<SymbolGroup> symbolBlockGroupSetBuilder = new ObjectOpenHashSet<>(keys.size());
        for (String symbolsID : keys) {
            final ConfigurationSection section = symbols.getConfigurationSection("symbols." + symbolsID);
            BlockType blockType = BlockType.valueOf(section.getString("mode").toUpperCase());
            Set<BlockFactor> blockFactor = getBlockFactors(section.getString("block_factor", ""));
            ObjectList<Condition> conditionList = getConditionList(section.getStringList("conditions"));
            ObjectList<Action> actionList = getActionList(section.getStringList("actions"));
            symbolBlockGroupSetBuilder.add(
                    new SymbolGroup(
                            symbolsID,
                            blockType,
                            blockFactor,
                            section.getStringList("symbols"),
                            section.getStringList("excluded_commands"),
                            conditionList,
                            actionList
                    )
            );
        }
        this.symbolBlockGroupSet = symbolBlockGroupSetBuilder;
    }

    public ObjectList<Action> getChatActions(ConfigurationSection section) {
        List<String> actionStrings = section.getStringList("actions");
        if (!actionStrings.isEmpty()) {
            return getActionList(section.getStringList("actions"));
        }
        ObjectList<String> actions = new ObjectArrayList<>();
        String message = section.getString("message");
        if (message != null) {
            actions.add("[MESSAGE] " + message);
        }
        String sound = section.getString("sound");
        if (sound != null) {
            actions.add("[SOUND] " + sound);
        }

        ConfigurationSection notifySection = section.getConfigurationSection("notify");
        if (notifySection != null && notifySection.getBoolean("enable")) {
            String notifyMessage = notifySection.getString("message");
            if (notifyMessage != null) {
                actions.add("[NOTIFY] " + notifyMessage);
            }
            String notifySound = notifySection.getString("sound");
            if (notifySound != null) {
                actions.add("[NOTIFY_SOUND] " + notifySound);
            }
        }
        return getActionList(actions);
    }

    private ObjectList<Action> getActionList(List<String> actionStrings) {
        ObjectList<Action> actionListBuilder = new ObjectArrayList<>(actionStrings.size());
        for (String actionString : actionStrings) {
            Action action = Action.fromString(actionString);
            if (action != null) {
                actionListBuilder.add(action);
            }
        }
        return actionListBuilder;
    }

    private ObjectList<Condition> getConditionList(List<String> conditionStrings) {
        ObjectList<Condition> conditionListBuilder = new ObjectArrayList<>(conditionStrings.size());
        for (String conditionString : conditionStrings) {
            Condition condition = Condition.fromString(conditionString);
            if (condition != null) {
                conditionListBuilder.add(condition);
            }
        }
        return conditionListBuilder;
    }

    private Set<BlockFactor> getBlockFactors(String blockFactorString) {
        Set<BlockFactor> blockFactors = EnumSet.noneOf(BlockFactor.class);
        for (String blockFactor : getWorkFactorsAsStringArray(blockFactorString)) {
            blockFactors.add(BlockFactor.valueOf(blockFactor.toUpperCase()));
        }
        return blockFactors;
    }

    public String[] getWorkFactorsAsStringArray(String str) {
        return str.trim().split(";");
    }

    public void setupExcluded(FileConfiguration config) {
        excludedPlayers = config.getConfigurationSection("settings").getBoolean("enable_excluded_players", false)
                ? Set.copyOf(config.getStringList("excluded_players"))
                : Set.of();
    }

    public FileConfiguration getFile(String path, String fileName) {
        File file = new File(path, fileName);
        if (!file.exists()) {
            try {
                if (path.equals(plugin.getDataFolder().getAbsolutePath())) {
                    plugin.saveResource(fileName, false);
                } else {
                    file.getParentFile().mkdirs();
                    try (InputStream in = plugin.getResource(fileName)) {
                        if (in == null) {
                            throw new IllegalArgumentException("Ресурс " + fileName + " не найден в папке плагина!");
                        }
                        Files.copy(in, file.toPath());
                    }
                }
            } catch (IOException ex) {
                throw new RuntimeException("Не удалось создать файл " + file.getAbsolutePath(), ex);
            }
        }
        return YamlConfiguration.loadConfiguration(file);
    }
}