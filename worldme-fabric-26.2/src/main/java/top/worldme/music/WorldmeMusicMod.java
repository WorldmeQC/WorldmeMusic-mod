package top.worldme.music;

import net.fabricmc.api.ModInitializer;
import top.worldme.music.manager.SoundManagerImpl;

/**
 * Mod 主入口
 *
 * @author 真心
 * @email qgzhenxin@qq.com
 * @since 2023/1/28 13:01
 */
@SuppressWarnings("AlibabaClassNamingShouldBeCamel")
public class WorldmeMusicMod implements ModInitializer {

    @Override
    public void onInitialize() {
        WorldmeMusic.setSoundManager(new SoundManagerImpl());
        WorldmeMusic.onEnable();
    }
}