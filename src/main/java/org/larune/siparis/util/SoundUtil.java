package org.larune.siparis.util;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class SoundUtil {

    private SoundUtil() {}

    public static void playOrderCreated(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
    }

    public static void playDeliverySuccess(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    public static void playOrderCompleted(Player p) {
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
    }

    public static void playOrderCancelled(Player p) {
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.5f);
    }

    public static void playBoxWithdraw(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
    }

    public static void playMenuOpen(Player p) {
        p.playSound(p.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    public static void playClick(Player p) {
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }

    public static void playPageTurn(Player p) {
        p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.0f);
    }

    public static void playValueChange(Player p) {
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.3f, 1.5f);
    }

    public static void playError(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
    }

    public static void playNoMoney(Player p) {
        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.3f, 1.5f);
    }

    public static void playLimitReached(Player p) {
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f);
    }

    public static void playNewDeliveryNotification(Player p) {
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
    }

    public static void playExpiryWarning(Player p) {
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 0.5f);
    }

    public static void playOrderExpired(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.3f, 1.5f);
    }
}
