package earth.terrarium.heracles.forge;

import earth.terrarium.heracles.client.compat.rei.HeraclesReiClientPlugin;
import me.shedaniel.rei.forge.REIPluginClient;

/**
 * Bridge class for REI client plugin loading on Forge.
 *
 * This class exists so the REI Forge plugin loader can discover the client plugin.
 * It forwards to the shared `HeraclesReiClientPlugin` in `common`.
 */
@REIPluginClient
public class HeraclesReiForgeClientPlugin extends HeraclesReiClientPlugin {

}
