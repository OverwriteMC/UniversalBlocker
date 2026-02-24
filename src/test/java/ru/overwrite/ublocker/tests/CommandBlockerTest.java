package ru.overwrite.ublocker.tests;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import ru.overwrite.ublocker.UniversalBlocker;
import ru.overwrite.ublocker.actions.Action;
import ru.overwrite.ublocker.actions.ActionType;
import ru.overwrite.ublocker.blockgroups.BlockType;
import ru.overwrite.ublocker.blockgroups.CommandGroup;
import ru.overwrite.ublocker.color.Colorizer;
import ru.overwrite.ublocker.color.ColorizerProvider;
import ru.overwrite.ublocker.conditions.ConditionChecker;
import ru.overwrite.ublocker.configuration.Config;
import ru.overwrite.ublocker.listeners.commands.CommandBlocker;
import ru.overwrite.ublocker.task.runner.Runner;
import ru.overwrite.ublocker.utils.Utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.Mockito.*;

public class CommandBlockerTest {

    private UniversalBlocker plugin;
    private Runner runner;
    private CommandBlocker commandBlocker; // kept for compatibility but not used for per-test spying
    private MockedStatic<Bukkit> bukkitStatic;
    private MockedStatic<ConditionChecker> condStatic;
    private ConsoleCommandSender console;

    @BeforeMethod
    public void setUp() throws Exception {
        plugin = mock(UniversalBlocker.class);
        runner = mock(Runner.class);
        when(plugin.getRunner()).thenReturn(runner);

        // Runner.runAsync -> run synchronously
        doAnswer(invocation -> {
            Runnable r = invocation.getArgument(0);
            r.run();
            return null;
        }).when(runner).runAsync(any(Runnable.class));

        // default excluded = false
        when(plugin.isExcluded(any())).thenReturn(false);

        // initial instance (may be unused by tests that create fresh instance)
        commandBlocker = new CommandBlocker(plugin);

        // Utils flags
        Utils.USE_PAPI = false;
        Utils.DEBUG_COMMANDS = false;
        Utils.DEBUG_SYMBOLS = false;
        Utils.DEBUG_CHAT = false;
        Utils.IGNORE_UNKNOWN_COMMANDS = false;

        // mock Bukkit static calls
        bukkitStatic = Mockito.mockStatic(Bukkit.class);
        bukkitStatic.when(Bukkit::getOnlinePlayers).thenReturn(Set.of());
        console = mock(ConsoleCommandSender.class);
        bukkitStatic.when(Bukkit::getConsoleSender).thenReturn(console);
        bukkitStatic.when(() -> Bukkit.dispatchCommand(any(), anyString())).thenReturn(true);

        // mock ConditionChecker static to always return true for requirements
        condStatic = Mockito.mockStatic(ConditionChecker.class);
        condStatic.when(() -> ConditionChecker.isMeetsRequirements(any(), any())).thenReturn(true);

        // ensure ColorizerProvider.COLORIZER exists so formatActionMessage won't NPE
        Colorizer colorizerMock = mock(Colorizer.class);
        when(colorizerMock.colorize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        try {
            Field fld = ColorizerProvider.class.getDeclaredField("COLORIZER");
            fld.setAccessible(true);
            if (Modifier.isStatic(fld.getModifiers())) {
                fld.set(null, colorizerMock);
            } else {
                Object provider = ColorizerProvider.class.getDeclaredConstructor().newInstance();
                fld.set(provider, colorizerMock);
            }
        } catch (NoSuchFieldException ignored) {
            // ignore if not present; best-effort
        }
    }

    @AfterMethod
    public void tearDown() {
        bukkitStatic.close();
        condStatic.close();
    }

    private Action createAction(ActionType type, String context) {
        Action a = mock(Action.class);
        when(a.type()).thenReturn(type);
        when(a.context()).thenReturn(context);
        return a;
    }

    private PlayerCommandPreprocessEvent makeEvent(Player player, String message) {
        PlayerCommandPreprocessEvent event = mock(PlayerCommandPreprocessEvent.class, withSettings().extraInterfaces(Cancellable.class));
        when(event.getPlayer()).thenReturn(player);
        when(event.getMessage()).thenReturn(message);

        AtomicBoolean cancelled = new AtomicBoolean(false);
        doAnswer(invocation -> {
            boolean val = invocation.getArgument(0);
            cancelled.set(val);
            return null;
        }).when(event).setCancelled(anyBoolean());
        when(event.isCancelled()).thenAnswer(invocation -> cancelled.get());

        return event;
    }

    @Test
    public void testStringMode_detects_op_and_passes_correct_com_to_executeActions() {
        Player player = mock(Player.class);
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(player.getWorld()).thenReturn(world);
        when(player.getName()).thenReturn("Player1");

        PlayerCommandPreprocessEvent evt = makeEvent(player, "/op");

        // prepare Config with one CommandGroup in STRING mode using convenience ctor
        Action blockAction = createAction(ActionType.BLOCK, "[BLOCK]");
        CommandGroup group = new CommandGroup(
                "g1",
                BlockType.STRING,
                false,                 // blockAliases
                false,                 // whitelistMode
                List.of("op"),         // commandsToBlock (convenience ctor will create set)
                List.of(),             // conditions
                List.of(blockAction)   // actions
        );

        Config cfg = mock(Config.class);
        when(cfg.getCommandBlockGroupSet()).thenReturn(Set.of(group));
        when(plugin.getPluginConfig()).thenReturn(cfg);

        // create fresh CommandBlocker after stubbing plugin.getPluginConfig()
        CommandBlocker fresh = new CommandBlocker(plugin);
        CommandBlocker spyCb = spy(fresh);
        doNothing().when(spyCb).executeActions(any(), any(), anyString(), anyString(), anyList());

        CommandBlocker.FULL_LOCK = false;

        spyCb.onCommand(evt);

        ArgumentCaptor<String> comCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> fullCmdCaptor = ArgumentCaptor.forClass(String.class);
        verify(spyCb, times(1)).executeActions(any(), eq(player), comCaptor.capture(), fullCmdCaptor.capture(), anyList());

        assert comCaptor.getValue().equalsIgnoreCase("op");
        assert fullCmdCaptor.getValue().equals("/op");
    }

    @Test
    public void testStringMode_detects_minecraft_namespace_and_aliases_behaviour() {
        Player player = mock(Player.class);
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(player.getWorld()).thenReturn(world);
        when(player.getName()).thenReturn("P");

        PlayerCommandPreprocessEvent evt = makeEvent(player, "/minecraft:op");

        Action blockAction = createAction(ActionType.BLOCK, "[BLOCK]");
        CommandGroup group = new CommandGroup(
                "g2",
                BlockType.STRING,
                false,
                false,
                List.of("minecraft:op"),
                List.of(),
                List.of(blockAction)
        );

        Config cfg = mock(Config.class);
        when(cfg.getCommandBlockGroupSet()).thenReturn(Set.of(group));
        when(plugin.getPluginConfig()).thenReturn(cfg);

        // create fresh CommandBlocker after stubbing plugin.getPluginConfig()
        CommandBlocker fresh = new CommandBlocker(plugin);
        CommandBlocker spyCb = spy(fresh);
        doNothing().when(spyCb).executeActions(any(), any(), anyString(), anyString(), anyList());

        CommandBlocker.FULL_LOCK = false;
        spyCb.onCommand(evt);

        ArgumentCaptor<String> comCaptor = ArgumentCaptor.forClass(String.class);
        verify(spyCb, times(1)).executeActions(any(), eq(player), comCaptor.capture(), anyString(), anyList());
        assert comCaptor.getValue().equalsIgnoreCase("minecraft:op");
    }

    @Test
    public void testPatternMode_detects_ver_and_pl_patterns() {
        Player player = mock(Player.class);
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(player.getWorld()).thenReturn(world);
        when(player.getName()).thenReturn("Patt");

        PlayerCommandPreprocessEvent evtVer = makeEvent(player, "/ver");
        PlayerCommandPreprocessEvent evtPl = makeEvent(player, "/pl");

        Action blockAction = createAction(ActionType.BLOCK, "[BLOCK]");
        // Use convenience constructor with pattern strings
        CommandGroup group = new CommandGroup(
                "g3",
                BlockType.PATTERN,
                false,                      // blockAliases ignored for patterns
                false,
                List.of("pl(ugins)?", "ver(sion)?"), // will be converted to Pattern set
                List.of(),
                List.of(blockAction)
        );

        Config cfg = mock(Config.class);
        when(cfg.getCommandBlockGroupSet()).thenReturn(Set.of(group));
        when(plugin.getPluginConfig()).thenReturn(cfg);

        // create fresh CommandBlocker after stubbing plugin.getPluginConfig()
        CommandBlocker fresh = new CommandBlocker(plugin);
        CommandBlocker spyCb = spy(fresh);
        doNothing().when(spyCb).executeActions(any(), any(), anyString(), anyString(), anyList());

        CommandBlocker.FULL_LOCK = false;

        spyCb.onCommand(evtVer);
        ArgumentCaptor<String> comCaptor1 = ArgumentCaptor.forClass(String.class);
        verify(spyCb, times(1)).executeActions(any(), eq(player), comCaptor1.capture(), eq("/ver"), anyList());
        assert comCaptor1.getValue().equalsIgnoreCase("ver");

        // reset spy invocations and verify /pl
        reset(spyCb);
        doNothing().when(spyCb).executeActions(any(), any(), anyString(), anyString(), anyList());
        spyCb.onCommand(evtPl);
        ArgumentCaptor<String> comCaptor2 = ArgumentCaptor.forClass(String.class);
        verify(spyCb, times(1)).executeActions(any(), eq(player), comCaptor2.capture(), eq("/pl"), anyList());
        assert comCaptor2.getValue().equalsIgnoreCase("pl");
    }

    @Test
    public void testWhitelistMode_allows_listed_and_blocks_others() {
        Player player = mock(Player.class);
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(player.getWorld()).thenReturn(world);
        when(player.getName()).thenReturn("WPlayer");

        // Event for a command that is in whitelist list
        PlayerCommandPreprocessEvent evtAllowed = makeEvent(player, "/op");
        // Event for a command that is NOT in whitelist list
        PlayerCommandPreprocessEvent evtBlocked = makeEvent(player, "/other");

        Action blockAction = createAction(ActionType.BLOCK, "[BLOCK]");

        // Create CommandGroup with whitelistMode = true and commands list contains "op"
        CommandGroup whitelistGroup = new CommandGroup(
                "whitelistGroup",
                BlockType.STRING,
                false,
                true,                    // whitelistMode = true
                List.of("op"),           // allowed commands in whitelist
                List.of(),
                List.of(blockAction)
        );

        Config cfg = mock(Config.class);
        when(cfg.getCommandBlockGroupSet()).thenReturn(Set.of(whitelistGroup));
        when(plugin.getPluginConfig()).thenReturn(cfg);

        // create fresh CommandBlocker after stubbing plugin.getPluginConfig()
        CommandBlocker fresh = new CommandBlocker(plugin);
        CommandBlocker spyCb = spy(fresh);
        doNothing().when(spyCb).executeActions(any(), any(), anyString(), anyString(), anyList());

        CommandBlocker.FULL_LOCK = false;

        // Command "/op" is in whitelist -> should NOT trigger executeActions
        spyCb.onCommand(evtAllowed);
        verify(spyCb, never()).executeActions(any(), eq(player), anyString(), anyString(), anyList());

        // Command "/other" is not in whitelist -> should trigger executeActions (blocked)
        reset(spyCb);
        doNothing().when(spyCb).executeActions(any(), any(), anyString(), anyString(), anyList());
        spyCb.onCommand(evtBlocked);
        verify(spyCb, times(1)).executeActions(any(), eq(player), anyString(), eq("/other"), anyList());
    }

    @Test
    public void testExcluded_player_skips_all_processing() {
        Player player = mock(Player.class);
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(player.getWorld()).thenReturn(world);
        when(player.getName()).thenReturn("ExcludedPlayer");

        PlayerCommandPreprocessEvent evt = makeEvent(player, "/op");

        // Configure config with a blocking group for "op"
        Action blockAction = createAction(ActionType.BLOCK, "[BLOCK]");
        CommandGroup group = new CommandGroup(
                "g-exclude",
                BlockType.STRING,
                false,
                false,
                List.of("op"),
                List.of(),
                List.of(blockAction)
        );

        Config cfg = mock(Config.class);
        when(cfg.getCommandBlockGroupSet()).thenReturn(Set.of(group));
        when(plugin.getPluginConfig()).thenReturn(cfg);

        // Make plugin.isExcluded return true for this player
        when(plugin.isExcluded(player)).thenReturn(true);

        // create fresh CommandBlocker after stubbing plugin.getPluginConfig()
        CommandBlocker fresh = new CommandBlocker(plugin);
        CommandBlocker spyCb = spy(fresh);
        doNothing().when(spyCb).executeActions(any(), any(), anyString(), anyString(), anyList());

        CommandBlocker.FULL_LOCK = false;

        spyCb.onCommand(evt);

        // Because player is excluded, executeActions must not be invoked
        verify(spyCb, never()).executeActions(any(), any(), anyString(), anyString(), anyList());

        // Restore plugin.isExcluded default for other tests
        when(plugin.isExcluded(any())).thenReturn(false);
    }
}
