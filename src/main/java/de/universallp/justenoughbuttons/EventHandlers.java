package de.universallp.justenoughbuttons;

import com.mojang.realmsclient.gui.ChatFormatting;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.List;

import static de.universallp.justenoughbuttons.JEIButtons.*;

/**
 * Created by universallp on 11.08.2016 16:07.
 * This file is part of JustEnoughButtons which is licenced
 * under the MOZILLA PUBLIC LICENCE 2.0 - mozilla.org/en-US/MPL/2.0/
 * github.com/UniversalLP/JustEnoughButtons
 */
public class EventHandlers {

    private static boolean gameRuleDayCycle = false;
    private boolean isMouseDown = false;
    private static BlockPos lastPlayerPos = null;

    private boolean drawOverlay = false;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onGuiDraw(GuiScreenEvent.DrawScreenEvent e) {
        if (JEIButtons.configHasChanged) {
            JEIButtons.configHasChanged = false;
            setUpPositions();
        }

        if (e.getGui() != null && e.getGui() instanceof GuiContainer) {
            GuiContainer g = (GuiContainer) e.getGui();
            EntityPlayerSP pl = ClientProxy.player;
            if (pl.inventory.getItemStack() == null) {
                JEIButtons.btnTrash.setEnabled(false);
            } else {
                JEIButtons.btnTrash.setEnabled(true);
            }

            if (btnGameMode == EnumButtonCommands.SPECTATE && !ConfigHandler.enableSpectatoreMode || btnGameMode == EnumButtonCommands.ADVENTURE && !ConfigHandler.enableAdventureMode)
                btnGameMode = btnGameMode.cycle();

            JEIButtons.isAnyButtonHovered = false;
            gameRuleDayCycle = ClientProxy.mc.theWorld.getGameRules().getBoolean("doDaylightCycle");
            btnGameMode.draw(g);
            btnTrash.draw(g);
            btnSun.draw(g);
            btnRain.draw(g);
            btnDay.draw(g);
            btnNight.draw(g);
            btnNoMobs.draw(g);
            btnFreeze.draw(g);

            for (EnumButtonCommands btn : btnCustom)
                btn.draw(g);

            adjustGamemode();

            if (JEIButtons.isAnyButtonHovered) {
                List<String> tip = getTooltip(JEIButtons.hoveredButton);
                if (tip != null) {
                    int mouseY = JEIButtons.proxy.getMouseY();
                    GuiUtils.drawHoveringText(tip, JEIButtons.proxy.getMouseX(), mouseY < 17 ? 17 : mouseY, ClientProxy.mc.displayWidth, ClientProxy.mc.displayHeight, -1, ClientProxy.mc.fontRendererObj);
                    RenderHelper.disableStandardItemLighting();
                }

                if (Mouse.isButtonDown(0) && !isMouseDown && JEIButtons.hoveredButton.isEnabled) {
                    isMouseDown = true;
                    String command = "/" + JEIButtons.hoveredButton.getCommand();

                    if (JEIButtons.hoveredButton == EnumButtonCommands.FREEZETIME) {
                        command += " " + (gameRuleDayCycle ? "false" : "true");
                    }

                    if (JEIButtons.hoveredButton == EnumButtonCommands.DELETE) {

                        ItemStack draggedStack = pl.inventory.getItemStack();
                        String name  = draggedStack.getItem().getRegistryName().toString();

                        command += pl.getDisplayName().getUnformattedText() + " " + name;

                        if (!GuiScreen.isShiftKeyDown()) {
                            int data = draggedStack.getItemDamage();
                            command += " " + data;
                        }

                        if (draggedStack.stackSize == 0)
                            pl.inventory.setItemStack(null);
                    }



                    pl.sendChatMessage(command);
                    ClientProxy.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));

                    if (JEIButtons.hoveredButton.ordinal() < 4) // Game mode buttons
                        JEIButtons.btnGameMode = hoveredButton.cycle();
                } else if (!Mouse.isButtonDown(0) && isMouseDown) {
                    isMouseDown = false;
                }
            }
        }
    }

    private void adjustGamemode() {
        GameType t = ClientProxy.mc.playerController.getCurrentGameType();
        boolean doSwitch = false;

        if (t == GameType.CREATIVE && btnGameMode == EnumButtonCommands.CREATIVE)
            doSwitch = true;
        else if (t == GameType.SURVIVAL && btnGameMode == EnumButtonCommands.SURVIVAL)
            doSwitch = true;
        else if (t == GameType.ADVENTURE && btnGameMode == EnumButtonCommands.ADVENTURE)
            doSwitch = true;

        else if (t == GameType.SPECTATOR && btnGameMode == EnumButtonCommands.SPECTATE)
            doSwitch = true;

        if (doSwitch)
            btnGameMode = btnGameMode.cycle();
    }

    @SubscribeEvent
    public void onWorldJoin(EntityJoinWorldEvent e) {
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT)
            if (e.getEntity() instanceof EntityPlayer) {
                ClientProxy.player = FMLClientHandler.instance().getClientPlayerEntity();
                if (((EntityPlayer) e.getEntity()).capabilities.isCreativeMode) {
                    JEIButtons.btnGameMode = btnGameMode.cycle();
                } else {
                    JEIButtons.btnGameMode = EnumButtonCommands.CREATIVE;
                }
            }
    }

    @SubscribeEvent
    public void handleKeyInputEvent(GuiScreenEvent.KeyboardInputEvent e) {
        GuiScreen gui = ClientProxy.mc.currentScreen;
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT)
            if (gui != null && gui instanceof GuiContainer && Keyboard.isKeyDown(ClientProxy.makeCopyKey.getKeyCode())) {
                Slot hovered = ((GuiContainer) gui).getSlotUnderMouse();

                if (ClientProxy.player.inventory.getItemStack() == null && hovered != null && hovered.getHasStack()) {
                    ItemStack stack = hovered.getStack().copy();
                    if (ClientProxy.player.capabilities.isCreativeMode)
                        stack.stackSize = 0;
                    else
                        stack.stackSize = 1;
                    ClientProxy.player.inventory.setItemStack(stack);
                }
            }
    }

    @SubscribeEvent
    public void onMouseEvent(GuiScreenEvent.MouseInputEvent event) {
         if (Mouse.getEventButton() == 0 && JEIButtons.isAnyButtonHovered && JEIButtons.hoveredButton == EnumButtonCommands.DELETE) {
            if (ClientProxy.player != null && ClientProxy.player.inventory.getItemStack() != null) {
                if (event.isCancelable())
                    event.setCanceled(true);
                event.setResult(Event.Result.DENY);
            }
        }
    }

    @SubscribeEvent
    public void onKeyPressed(InputEvent.KeyInputEvent event) {
        if (ClientProxy.mobOverlay.isKeyDown()) {
            drawOverlay = !drawOverlay;
        }

        if (!drawOverlay) {
            MobOverlayRenderer.clearCache();
            lastPlayerPos = null;
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (drawOverlay && FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
            if (lastPlayerPos == null || !lastPlayerPos.equals(ClientProxy.player.getPosition())) {
                MobOverlayRenderer.cacheMobSpawns(ClientProxy.player);
                lastPlayerPos = ClientProxy.player.getPosition();
            }
        }
    }

    @SubscribeEvent
    public void onWorldDraw(RenderWorldLastEvent event) {
        if (drawOverlay)
            MobOverlayRenderer.renderMobSpawnOverlay();
    }

    public List<String> getTooltip(EnumButtonCommands btn) {
        ArrayList<String> list = new ArrayList<String>();
        switch (btn) {

            case ADVENTURE:
                list.add(I18n.format("justenoughbuttons.switchto", I18n.format("gameMode.adventure")));
                break;
            case CREATIVE:
                list.add(I18n.format("justenoughbuttons.switchto", I18n.format("gameMode.creative")));
                break;
            case SPECTATE:
                list.add(I18n.format("justenoughbuttons.switchto", I18n.format("gameMode.spectator")));
                break;
            case SURVIVAL:
                list.add(I18n.format("justenoughbuttons.switchto", I18n.format("gameMode.survival")));
                break;
            case DAY:
                list.add(I18n.format("justenoughbuttons.switchto", I18n.format("justenoughbuttons.timeday")));
                break;
            case NIGHT:
                list.add(I18n.format("justenoughbuttons.switchto", I18n.format("justenoughbuttons.timenight")));
                break;
            case DELETE:
                ItemStack draggedStack = ClientProxy.player.inventory.getItemStack();
                if (draggedStack != null) {
                    list.add(I18n.format("justenoughbuttons.deleteall", I18n.format(draggedStack.getUnlocalizedName() + ".name")));
                    if (GuiScreen.isShiftKeyDown())
                        list.add(ChatFormatting.GRAY + I18n.format("justenoughbuttons.ignoringmeta"));
                } else {
                    list.add(I18n.format("justenoughbuttons.dragitemshere"));
                    list.add(ChatFormatting.GRAY + I18n.format("justenoughbuttons.holdshift"));
                }
                break;
            case FREEZETIME:
                if (gameRuleDayCycle)
                    list.add(I18n.format("justenoughbuttons.freezetime"));
                else
                    list.add(I18n.format("justenoughbuttons.unfreezetime"));
                break;
            case NOMOBS:
                list.add(I18n.format("justenoughbuttons.nomobs"));
                break;
            case RAIN:
                list.add(I18n.format("commands.weather.rain"));
                break;
            case SUN:
                list.add(I18n.format("commands.weather.clear"));
                break;
            case CUSTOM1:
            case CUSTOM2:
            case CUSTOM3:
            case CUSTOM4:
                if (ConfigHandler.customName[btn.id].equals(""))
                    list.add(I18n.format("justenoughbuttons.customcommand", "/" + ConfigHandler.customCommand[btn.id]));
                else
                    list.add(ConfigHandler.customName[btn.id]);
                break;
        }

        return list;
    }
}