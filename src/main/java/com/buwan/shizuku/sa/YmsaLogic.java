package com.buwan.shizuku.sa;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 夜冥杀里可单测的纯逻辑：是否该杀（排除施法者/自己宠物/已移除/已死）、
 * boss 关键词匹配、保护标签判断、清理表推进等。
 *
 * 把判定抽出来是为了避免在 900 行的 Ymsa 里调试。
 */
final class YmsaLogic {
    private YmsaLogic() {
    }

    public static boolean shouldKillPure(boolean isCaster, boolean isOwnPet, boolean isRemoved, boolean isDead) {
        return !isCaster && !isOwnPet && !isRemoved && !isDead;
    }

    public static boolean matchesBossKeywordsPure(String lowerClassName, String lowerDisplayName, List<String> classFragments, List<String> nameFragments) {
        if (lowerClassName != null && classFragments != null) {
            for (String fragment : classFragments) {
                if (fragment != null && lowerClassName.contains(fragment)) {
                    return true;
                }
            }
        }

        if (lowerDisplayName != null && nameFragments != null) {
            for (String fragmentx : nameFragments) {
                if (fragmentx != null && lowerDisplayName.contains(fragmentx)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean anyTagPresent(Set<String> presentKeys, List<String> protectionTags) {
        if (presentKeys != null && protectionTags != null) {
            for (String tag : protectionTags) {
                if (presentKeys.contains(tag)) {
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    public static void tickCleanups(Map<UUID, YmsaLogic.CleanupEntry> map) {
        if (map != null && !map.isEmpty()) {
            map.entrySet().removeIf(entry -> {
                entry.getValue().ticksLeft--;
                return entry.getValue().ticksLeft <= 0;
            });
        }
    }

    public static void tickTypeSuppression(Map<String, Integer> map) {
        if (map != null && !map.isEmpty()) {
            map.replaceAll((key, count) -> count - 1);
            map.entrySet().removeIf(entry -> entry.getValue() <= 0);
        }
    }

    public static void filterByKiller(Map<UUID, YmsaLogic.CleanupEntry> map, UUID killerUuid) {
        if (map != null && !map.isEmpty() && killerUuid != null) {
            map.entrySet().removeIf(entry -> killerUuid.equals(entry.getValue().killerUuid));
        }
    }

    public static final class CleanupEntry {
        public final UUID killerUuid;
        public int ticksLeft;

        public CleanupEntry(UUID killerUuid, int ticksLeft) {
            this.killerUuid = killerUuid;
            this.ticksLeft = ticksLeft;
        }
    }
}
