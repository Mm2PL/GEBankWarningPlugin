package gebankwarning;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ItemID;
import net.runelite.api.ScriptID;
import net.runelite.api.Varbits;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.util.Arrays;

import static net.runelite.client.util.QuantityFormatter.formatNumber;

@Slf4j
@PluginDescriptor(
        name = "Banked GP Warning"
)
public class GEBankWarningPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private GEBankWarningConfig config;

    @Override
    protected void startUp() throws Exception {
        log.info("Example started!");
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("Example stopped!");
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
    }

    @Subscribe
    public void onScriptPostFired(ScriptPostFired event) {
        // GE: update offer
        if (event.getScriptId() == 779) {
            updateWarning();
        }
    }

    public void updateWarning() {
        // only for buying
        if (client.getVarbitValue(Varbits.GE_OFFER_CREATION_TYPE) != 0) {
            return;
        }

        var w = client.getWidget(WidgetInfo.GRAND_EXCHANGE_OFFER_CONTAINER);
        if (w == null) {
            return;
        }
        var children = w.getDynamicChildren();
        // Ensure the right UI is open
        if (children.length != 55) {
            return;
        }
        var button = children[children.length - 1];
        if (button == null) {
            return;
        }
        var priceText = children[39].getText();
        // when setting up the offer before the item is set both price and qty is ""
        if (priceText.equals("")) {
            return;
        }

        var price = Integer.parseInt(children[39].getText().replaceAll(" coins", "").replaceAll(",", ""));
        var qty = Integer.parseInt(children[32].getText().replaceAll(",", ""));

        var gpInInv = Arrays.stream(client.getWidget(WidgetInfo.GRAND_EXCHANGE_INVENTORY_ITEMS_CONTAINER)
                                          .getDynamicChildren())
                            .mapToInt(widget -> {
                                if (widget.isHidden()) {
                                    return 0;
                                }
                                if (widget.getItemId() == ItemID.COINS_995) {
                                    return widget.getItemQuantity();
                                } else if (widget.getItemId() == ItemID.PLATINUM_TOKEN) {
                                    return widget.getItemQuantity() * 1000;
                                }
                                return 0;
                            }).sum();
        var offerPrice = price * qty;
        log.info("Have {} coins; offer costs {}", gpInInv, offerPrice);
        var infoLine = children[43];
        if (offerPrice > gpInInv) {
            infoLine.setText(formatNumber(offerPrice) + " coins (" + formatNumber(offerPrice - gpInInv) + " from bank)");
            button.setText("Confirm");
            button.setTextColor(0xEF2929);
        }
    }

    @Provides
    GEBankWarningConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(GEBankWarningConfig.class);
    }
}
