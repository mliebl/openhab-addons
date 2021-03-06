/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.io.homekit.internal.accessories;

import static org.openhab.io.homekit.internal.HomekitCharacteristicType.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.items.GenericItem;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.library.items.ColorItem;
import org.eclipse.smarthome.core.library.items.DimmerItem;
import org.eclipse.smarthome.core.library.items.NumberItem;
import org.eclipse.smarthome.core.library.items.SwitchItem;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.io.homekit.internal.HomekitAccessoryUpdater;
import org.openhab.io.homekit.internal.HomekitCharacteristicType;
import org.openhab.io.homekit.internal.HomekitCommandType;
import org.openhab.io.homekit.internal.HomekitException;
import org.openhab.io.homekit.internal.HomekitTaggedItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.hapjava.characteristics.Characteristic;
import io.github.hapjava.characteristics.CharacteristicEnum;
import io.github.hapjava.characteristics.ExceptionalConsumer;
import io.github.hapjava.characteristics.HomekitCharacteristicChangeCallback;
import io.github.hapjava.characteristics.impl.audio.VolumeCharacteristic;
import io.github.hapjava.characteristics.impl.battery.StatusLowBatteryCharacteristic;
import io.github.hapjava.characteristics.impl.battery.StatusLowBatteryEnum;
import io.github.hapjava.characteristics.impl.carbondioxidesensor.CarbonDioxideLevelCharacteristic;
import io.github.hapjava.characteristics.impl.carbondioxidesensor.CarbonDioxidePeakLevelCharacteristic;
import io.github.hapjava.characteristics.impl.carbonmonoxidesensor.CarbonMonoxideLevelCharacteristic;
import io.github.hapjava.characteristics.impl.carbonmonoxidesensor.CarbonMonoxidePeakLevelCharacteristic;
import io.github.hapjava.characteristics.impl.common.NameCharacteristic;
import io.github.hapjava.characteristics.impl.common.ObstructionDetectedCharacteristic;
import io.github.hapjava.characteristics.impl.common.StatusActiveCharacteristic;
import io.github.hapjava.characteristics.impl.common.StatusFaultCharacteristic;
import io.github.hapjava.characteristics.impl.common.StatusFaultEnum;
import io.github.hapjava.characteristics.impl.common.StatusTamperedCharacteristic;
import io.github.hapjava.characteristics.impl.common.StatusTamperedEnum;
import io.github.hapjava.characteristics.impl.fan.*;
import io.github.hapjava.characteristics.impl.lightbulb.BrightnessCharacteristic;
import io.github.hapjava.characteristics.impl.lightbulb.ColorTemperatureCharacteristic;
import io.github.hapjava.characteristics.impl.lightbulb.HueCharacteristic;
import io.github.hapjava.characteristics.impl.lightbulb.SaturationCharacteristic;
import io.github.hapjava.characteristics.impl.valve.RemainingDurationCharacteristic;
import io.github.hapjava.characteristics.impl.valve.SetDurationCharacteristic;
import io.github.hapjava.characteristics.impl.windowcovering.CurrentHorizontalTiltAngleCharacteristic;
import io.github.hapjava.characteristics.impl.windowcovering.CurrentVerticalTiltAngleCharacteristic;
import io.github.hapjava.characteristics.impl.windowcovering.HoldPositionCharacteristic;
import io.github.hapjava.characteristics.impl.windowcovering.TargetHorizontalTiltAngleCharacteristic;
import io.github.hapjava.characteristics.impl.windowcovering.TargetVerticalTiltAngleCharacteristic;

/**
 * Creates a optional characteristics .
 *
 * @author Eugen Freiter - Initial contribution
 */
@NonNullByDefault
@SuppressWarnings("deprecation")
public class HomekitCharacteristicFactory {
    private static final Logger logger = LoggerFactory.getLogger(HomekitCharacteristicFactory.class);

    // List of optional characteristics and corresponding method to create them.
    private final static Map<HomekitCharacteristicType, BiFunction<HomekitTaggedItem, HomekitAccessoryUpdater, Characteristic>> optional = new HashMap<HomekitCharacteristicType, BiFunction<HomekitTaggedItem, HomekitAccessoryUpdater, Characteristic>>() {
        {
            put(NAME, HomekitCharacteristicFactory::createNameCharacteristic);
            put(BATTERY_LOW_STATUS, HomekitCharacteristicFactory::createStatusLowBatteryCharacteristic);
            put(FAULT_STATUS, HomekitCharacteristicFactory::createStatusFaultCharacteristic);
            put(TAMPERED_STATUS, HomekitCharacteristicFactory::createStatusTamperedCharacteristic);
            put(ACTIVE_STATUS, HomekitCharacteristicFactory::createStatusActiveCharacteristic);
            put(CARBON_MONOXIDE_LEVEL, HomekitCharacteristicFactory::createCarbonMonoxideLevelCharacteristic);
            put(CARBON_MONOXIDE_PEAK_LEVEL, HomekitCharacteristicFactory::createCarbonMonoxidePeakLevelCharacteristic);
            put(CARBON_DIOXIDE_LEVEL, HomekitCharacteristicFactory::createCarbonDioxideLevelCharacteristic);
            put(CARBON_DIOXIDE_PEAK_LEVEL, HomekitCharacteristicFactory::createCarbonDioxidePeakLevelCharacteristic);
            put(HOLD_POSITION, HomekitCharacteristicFactory::createHoldPositionCharacteristic);
            put(OBSTRUCTION_STATUS, HomekitCharacteristicFactory::createObstructionDetectedCharacteristic);
            put(CURRENT_HORIZONTAL_TILT_ANGLE,
                    HomekitCharacteristicFactory::createCurrentHorizontalTiltAngleCharacteristic);
            put(CURRENT_VERTICAL_TILT_ANGLE,
                    HomekitCharacteristicFactory::createCurrentVerticalTiltAngleCharacteristic);
            put(TARGET_HORIZONTAL_TILT_ANGLE,
                    HomekitCharacteristicFactory::createTargetHorizontalTiltAngleCharacteristic);
            put(TARGET_VERTICAL_TILT_ANGLE, HomekitCharacteristicFactory::createTargetVerticalTiltAngleCharacteristic);
            put(HUE, HomekitCharacteristicFactory::createHueCharacteristic);
            put(BRIGHTNESS, HomekitCharacteristicFactory::createBrightnessCharacteristic);
            put(SATURATION, HomekitCharacteristicFactory::createSaturationCharacteristic);
            put(COLOR_TEMPERATURE, HomekitCharacteristicFactory::createColorTemperatureCharacteristic);
            put(CURRENT_FAN_STATE, HomekitCharacteristicFactory::createCurrentFanStateCharacteristic);
            put(TARGET_FAN_STATE, HomekitCharacteristicFactory::createTargetFanStateCharacteristic);
            put(ROTATION_DIRECTION, HomekitCharacteristicFactory::createRotationDirectionCharacteristic);
            put(ROTATION_SPEED, HomekitCharacteristicFactory::createRotationSpeedCharacteristic);
            put(SWING_MODE, HomekitCharacteristicFactory::createSwingModeCharacteristic);
            put(LOCK_CONTROL, HomekitCharacteristicFactory::createLockPhysicalControlsCharacteristic);
            put(DURATION, HomekitCharacteristicFactory::createDurationCharacteristic);
            put(VOLUME, HomekitCharacteristicFactory::createVolumeCharacteristic);

            put(REMAINING_DURATION, HomekitCharacteristicFactory::createRemainingDurationCharacteristic);
            // LEGACY
            put(OLD_BATTERY_LOW_STATUS, HomekitCharacteristicFactory::createStatusLowBatteryCharacteristic);
        }
    };

    /**
     * create optional HomeKit characteristic
     *
     * @param item corresponding OH item
     * @param updater update to keep OH item and HomeKit characteristic in sync
     * @return HomeKit characteristic
     */
    public static Characteristic createCharacteristic(final HomekitTaggedItem item, HomekitAccessoryUpdater updater)
            throws HomekitException {
        final @Nullable HomekitCharacteristicType type = item.getCharacteristicType();
        logger.trace("createCharacteristic, type {} item {}", type, item);
        if (optional.containsKey(type)) {
            return optional.get(type).apply(item, updater);
        }
        logger.warn("Unsupported optional characteristic. Accessory type {}, characteristic type {}",
                item.getAccessoryType(), type);
        throw new HomekitException("Unsupported optional characteristic. Characteristic type \"" + type + "\"");
    }

    // METHODS TO CREATE SINGLE CHARACTERISTIC FROM OH ITEM

    // supporting methods
    @SuppressWarnings("null")
    private static <T extends CharacteristicEnum> CompletableFuture<T> getEnumFromItem(final HomekitTaggedItem item,
            T offEnum, T onEnum, T defaultEnum) {
        final State state = item.getItem().getState();
        if (state instanceof OnOffType) {
            return CompletableFuture.completedFuture(state.equals(OnOffType.OFF) ? offEnum : onEnum);
        } else if (state instanceof OpenClosedType) {
            return CompletableFuture.completedFuture(state.equals(OpenClosedType.CLOSED) ? offEnum : onEnum);
        } else if (state instanceof DecimalType) {
            return CompletableFuture.completedFuture(state.as(DecimalType.class).intValue() == 0 ? offEnum : onEnum);
        } else if (state instanceof UnDefType) {
            return CompletableFuture.completedFuture(defaultEnum);
        }
        logger.warn(
                "Item state {} is not supported. Only OnOffType,OpenClosedType and Decimal (0/1) are supported. Ignore item {}",
                state, item.getName());
        return CompletableFuture.completedFuture(defaultEnum);
    }

    private static void setValueFromEnum(final HomekitTaggedItem taggedItem, CharacteristicEnum value,
            CharacteristicEnum offEnum, CharacteristicEnum onEnum) {
        if (taggedItem.getItem() instanceof SwitchItem) {
            if (value.equals(offEnum)) {
                ((SwitchItem) taggedItem.getItem()).send(OnOffType.OFF);
            } else if (value.equals(onEnum)) {
                ((SwitchItem) taggedItem.getItem()).send(OnOffType.ON);
            } else {
                logger.warn("Enum value {} is not supported. Only following values are supported: {},{}", value,
                        offEnum, onEnum);
            }
        } else if (taggedItem.getItem() instanceof NumberItem) {
            ((NumberItem) taggedItem.getItem()).send(new DecimalType(value.getCode()));
        } else {
            logger.warn("Item type {} is not supported. Only Switch and Number item types are supported.",
                    taggedItem.getItem().getType());
        }
    }

    @SuppressWarnings("null")
    private static int getIntFromItem(final HomekitTaggedItem taggedItem) {
        int value = 0;
        final State state = taggedItem.getItem().getState();
        if (state instanceof PercentType) {
            value = state.as(PercentType.class).intValue();
        } else if (state instanceof DecimalType) {
            value = state.as(DecimalType.class).intValue();
        } else if (state instanceof UnDefType) {
            logger.debug("Item state {} is UNDEF {}.", state, taggedItem.getName());
        } else {
            logger.warn(
                    "Item state {} is not supported for {}. Only PercentType and DecimalType (0/100) are supported.",
                    state, taggedItem.getName());
        }
        return value;
    }

    private static Supplier<CompletableFuture<Integer>> getIntSupplier(final HomekitTaggedItem taggedItem) {
        return () -> CompletableFuture.completedFuture(getIntFromItem(taggedItem));
    }

    private static ExceptionalConsumer<Integer> setIntConsumer(final HomekitTaggedItem taggedItem) {
        return (value) -> {
            if (taggedItem.getItem() instanceof NumberItem) {
                ((NumberItem) taggedItem.getItem()).send(new DecimalType(value));
            } else {
                logger.warn("Item type {} is not supported for {}. Only Number type is supported.",
                        taggedItem.getItem().getType(), taggedItem.getName());
            }
        };
    }

    private static Supplier<CompletableFuture<Double>> getDoubleSupplier(final HomekitTaggedItem taggedItem) {
        return () -> {
            final DecimalType value = taggedItem.getItem().getStateAs(DecimalType.class);
            return CompletableFuture.completedFuture(value != null ? value.doubleValue() : 0.0);
        };
    }

    protected static Consumer<HomekitCharacteristicChangeCallback> getSubscriber(final HomekitTaggedItem taggedItem,
            final HomekitCharacteristicType key, final HomekitAccessoryUpdater updater) {
        return (callback) -> updater.subscribe((GenericItem) taggedItem.getItem(), key.getTag(), callback);
    }

    protected static Runnable getUnsubscriber(final HomekitTaggedItem taggedItem, final HomekitCharacteristicType key,
            final HomekitAccessoryUpdater updater) {
        return () -> updater.unsubscribe((GenericItem) taggedItem.getItem(), key.getTag());
    }

    // create method for characteristic
    private static StatusLowBatteryCharacteristic createStatusLowBatteryCharacteristic(
            final HomekitTaggedItem taggedItem, final HomekitAccessoryUpdater updater) {
        return new StatusLowBatteryCharacteristic(
                () -> getEnumFromItem(taggedItem, StatusLowBatteryEnum.NORMAL, StatusLowBatteryEnum.LOW,
                        StatusLowBatteryEnum.NORMAL),
                getSubscriber(taggedItem, BATTERY_LOW_STATUS, updater),
                getUnsubscriber(taggedItem, BATTERY_LOW_STATUS, updater));
    }

    private static StatusFaultCharacteristic createStatusFaultCharacteristic(final HomekitTaggedItem taggedItem,
            final HomekitAccessoryUpdater updater) {
        return new StatusFaultCharacteristic(
                () -> getEnumFromItem(taggedItem, StatusFaultEnum.NO_FAULT, StatusFaultEnum.GENERAL_FAULT,
                        StatusFaultEnum.NO_FAULT),
                getSubscriber(taggedItem, FAULT_STATUS, updater), getUnsubscriber(taggedItem, FAULT_STATUS, updater));
    }

    private static StatusTamperedCharacteristic createStatusTamperedCharacteristic(final HomekitTaggedItem taggedItem,
            final HomekitAccessoryUpdater updater) {
        return new StatusTamperedCharacteristic(
                () -> getEnumFromItem(taggedItem, StatusTamperedEnum.NOT_TAMPERED, StatusTamperedEnum.TAMPERED,
                        StatusTamperedEnum.NOT_TAMPERED),
                getSubscriber(taggedItem, TAMPERED_STATUS, updater),
                getUnsubscriber(taggedItem, TAMPERED_STATUS, updater));
    }

    private static ObstructionDetectedCharacteristic createObstructionDetectedCharacteristic(
            final HomekitTaggedItem taggedItem, HomekitAccessoryUpdater updater) {
        return new ObstructionDetectedCharacteristic(
                () -> CompletableFuture.completedFuture(taggedItem.getItem().getState() == OnOffType.ON
                        || taggedItem.getItem().getState() == OpenClosedType.OPEN),
                getSubscriber(taggedItem, OBSTRUCTION_STATUS, updater),
                getUnsubscriber(taggedItem, OBSTRUCTION_STATUS, updater));
    }

    private static StatusActiveCharacteristic createStatusActiveCharacteristic(final HomekitTaggedItem taggedItem,
            HomekitAccessoryUpdater updater) {
        return new StatusActiveCharacteristic(
                () -> CompletableFuture.completedFuture(taggedItem.getItem().getState() == OnOffType.ON
                        || taggedItem.getItem().getState() == OpenClosedType.OPEN),
                getSubscriber(taggedItem, ACTIVE_STATUS, updater), getUnsubscriber(taggedItem, ACTIVE_STATUS, updater));
    }

    private static NameCharacteristic createNameCharacteristic(final HomekitTaggedItem taggedItem,
            HomekitAccessoryUpdater updater) {
        return new NameCharacteristic(() -> {
            final State state = taggedItem.getItem().getState();
            return CompletableFuture.completedFuture(state instanceof UnDefType ? "" : state.toString());
        });
    }

    private static HoldPositionCharacteristic createHoldPositionCharacteristic(final HomekitTaggedItem taggedItem,
            HomekitAccessoryUpdater updater) {
        return new HoldPositionCharacteristic(OnOffType::from);
    }

    private static CarbonMonoxideLevelCharacteristic createCarbonMonoxideLevelCharacteristic(
            final HomekitTaggedItem taggedItem, HomekitAccessoryUpdater updater) {
        return new CarbonMonoxideLevelCharacteristic(getDoubleSupplier(taggedItem),
                getSubscriber(taggedItem, CARBON_DIOXIDE_LEVEL, updater),
                getUnsubscriber(taggedItem, CARBON_DIOXIDE_LEVEL, updater));
    }

    private static CarbonMonoxidePeakLevelCharacteristic createCarbonMonoxidePeakLevelCharacteristic(
            final HomekitTaggedItem taggedItem, HomekitAccessoryUpdater updater) {
        return new CarbonMonoxidePeakLevelCharacteristic(getDoubleSupplier(taggedItem),
                getSubscriber(taggedItem, CARBON_DIOXIDE_PEAK_LEVEL, updater),
                getUnsubscriber(taggedItem, CARBON_DIOXIDE_PEAK_LEVEL, updater));
    }

    private static CarbonDioxideLevelCharacteristic createCarbonDioxideLevelCharacteristic(
            final HomekitTaggedItem taggedItem, HomekitAccessoryUpdater updater) {
        return new CarbonDioxideLevelCharacteristic(getDoubleSupplier(taggedItem),
                getSubscriber(taggedItem, CARBON_MONOXIDE_LEVEL, updater),
                getUnsubscriber(taggedItem, CARBON_MONOXIDE_LEVEL, updater));
    }

    private static CarbonDioxidePeakLevelCharacteristic createCarbonDioxidePeakLevelCharacteristic(
            final HomekitTaggedItem taggedItem, HomekitAccessoryUpdater updater) {
        return new CarbonDioxidePeakLevelCharacteristic(getDoubleSupplier(taggedItem),
                getSubscriber(taggedItem, CARBON_MONOXIDE_PEAK_LEVEL, updater),
                getUnsubscriber(taggedItem, CARBON_MONOXIDE_PEAK_LEVEL, updater));
    }

    private static CurrentHorizontalTiltAngleCharacteristic createCurrentHorizontalTiltAngleCharacteristic(
            final HomekitTaggedItem taggedItem, HomekitAccessoryUpdater updater) {
        return new CurrentHorizontalTiltAngleCharacteristic(getIntSupplier(taggedItem),
                getSubscriber(taggedItem, CURRENT_HORIZONTAL_TILT_ANGLE, updater),
                getUnsubscriber(taggedItem, CURRENT_HORIZONTAL_TILT_ANGLE, updater));
    }

    private static CurrentVerticalTiltAngleCharacteristic createCurrentVerticalTiltAngleCharacteristic(
            final HomekitTaggedItem taggedItem, HomekitAccessoryUpdater updater) {
        return new CurrentVerticalTiltAngleCharacteristic(getIntSupplier(taggedItem),
                getSubscriber(taggedItem, CURRENT_VERTICAL_TILT_ANGLE, updater),
                getUnsubscriber(taggedItem, CURRENT_VERTICAL_TILT_ANGLE, updater));
    }

    private static TargetHorizontalTiltAngleCharacteristic createTargetHorizontalTiltAngleCharacteristic(
            final HomekitTaggedItem taggedItem, HomekitAccessoryUpdater updater) {
        return new TargetHorizontalTiltAngleCharacteristic(getIntSupplier(taggedItem), setIntConsumer(taggedItem),
                getSubscriber(taggedItem, TARGET_HORIZONTAL_TILT_ANGLE, updater),
                getUnsubscriber(taggedItem, TARGET_HORIZONTAL_TILT_ANGLE, updater));
    }

    private static TargetVerticalTiltAngleCharacteristic createTargetVerticalTiltAngleCharacteristic(
            final HomekitTaggedItem taggedItem, HomekitAccessoryUpdater updater) {
        return new TargetVerticalTiltAngleCharacteristic(getIntSupplier(taggedItem), setIntConsumer(taggedItem),
                getSubscriber(taggedItem, TARGET_HORIZONTAL_TILT_ANGLE, updater),
                getUnsubscriber(taggedItem, TARGET_HORIZONTAL_TILT_ANGLE, updater));
    }

    private static HueCharacteristic createHueCharacteristic(final HomekitTaggedItem taggedItem,
            HomekitAccessoryUpdater updater) {
        return new HueCharacteristic(() -> {
            Double value = 0.0;
            State state = taggedItem.getItem().getState();
            if (state instanceof HSBType) {
                value = ((HSBType) state).getHue().doubleValue();
            }
            return CompletableFuture.completedFuture(value);
        }, (hue) -> {
            if (taggedItem.getItem() instanceof ColorItem) {
                taggedItem.sendCommandProxy(HomekitCommandType.HUE_COMMAND, new DecimalType(hue));
            } else {
                logger.warn("Item type {} is not supported for {}. Only Color type is supported.",
                        taggedItem.getItem().getType(), taggedItem.getName());
            }
        }, getSubscriber(taggedItem, HUE, updater), getUnsubscriber(taggedItem, HUE, updater));
    }

    private static BrightnessCharacteristic createBrightnessCharacteristic(final HomekitTaggedItem taggedItem,
            HomekitAccessoryUpdater updater) {
        return new BrightnessCharacteristic(() -> {
            int value = 0;
            final State state = taggedItem.getItem().getState();
            if (state instanceof HSBType) {
                value = ((HSBType) state).getBrightness().intValue();
            } else if (state instanceof PercentType) {
                value = ((PercentType) state).intValue();
            }
            return CompletableFuture.completedFuture(value);
        }, (brightness) -> {
            final Item item = taggedItem.getItem();
            if (item instanceof DimmerItem) {
                taggedItem.sendCommandProxy(HomekitCommandType.BRIGHTNESS_COMMAND, new PercentType(brightness));
            } else {
                logger.warn("Item type {} is not supported for {}. Only ColorItem and DimmerItem are supported.",
                        item.getType(), taggedItem.getName());
            }
        }, getSubscriber(taggedItem, BRIGHTNESS, updater), getUnsubscriber(taggedItem, BRIGHTNESS, updater));
    }

    private static SaturationCharacteristic createSaturationCharacteristic(final HomekitTaggedItem taggedItem,
            HomekitAccessoryUpdater updater) {
        return new SaturationCharacteristic(() -> {
            Double value = 0.0;
            State state = taggedItem.getItem().getState();
            if (state instanceof HSBType) {
                value = ((HSBType) state).getSaturation().doubleValue();
            } else if (state instanceof PercentType) {
                value = ((PercentType) state).doubleValue();
            }
            return CompletableFuture.completedFuture(value);
        }, (saturation) -> {
            if (taggedItem.getItem() instanceof ColorItem) {
                taggedItem.sendCommandProxy(HomekitCommandType.SATURATION_COMMAND,
                        new PercentType(saturation.intValue()));
            } else {
                logger.warn("Item type {} is not supported for {}. Only Color type is supported.",
                        taggedItem.getItem().getType(), taggedItem.getName());
            }
        }, getSubscriber(taggedItem, SATURATION, updater), getUnsubscriber(taggedItem, SATURATION, updater));
    }

    private static ColorTemperatureCharacteristic createColorTemperatureCharacteristic(
            final HomekitTaggedItem taggedItem, HomekitAccessoryUpdater updater) {
        return new ColorTemperatureCharacteristic(getIntSupplier(taggedItem), setIntConsumer(taggedItem),
                getSubscriber(taggedItem, COLOR_TEMPERATURE, updater),
                getUnsubscriber(taggedItem, COLOR_TEMPERATURE, updater));
    }

    private static CurrentFanStateCharacteristic createCurrentFanStateCharacteristic(final HomekitTaggedItem taggedItem,
            HomekitAccessoryUpdater updater) {
        return new CurrentFanStateCharacteristic(() -> {
            final DecimalType value = taggedItem.getItem().getStateAs(DecimalType.class);
            CurrentFanStateEnum currentFanStateEnum = value != null ? CurrentFanStateEnum.fromCode(value.intValue())
                    : null;
            if (currentFanStateEnum == null) {
                currentFanStateEnum = CurrentFanStateEnum.INACTIVE;
            }
            return CompletableFuture.completedFuture(currentFanStateEnum);
        }, getSubscriber(taggedItem, CURRENT_FAN_STATE, updater),
                getUnsubscriber(taggedItem, CURRENT_FAN_STATE, updater));
    }

    private static TargetFanStateCharacteristic createTargetFanStateCharacteristic(final HomekitTaggedItem taggedItem,
            HomekitAccessoryUpdater updater) {
        return new TargetFanStateCharacteristic(() -> {
            final DecimalType value = taggedItem.getItem().getStateAs(DecimalType.class);
            TargetFanStateEnum targetFanStateEnum = value != null ? TargetFanStateEnum.fromCode(value.intValue())
                    : null;
            if (targetFanStateEnum == null) {
                targetFanStateEnum = TargetFanStateEnum.AUTO;
            }
            return CompletableFuture.completedFuture(targetFanStateEnum);
        }, (targetState) -> {
            if (taggedItem.getItem() instanceof NumberItem) {
                ((NumberItem) taggedItem.getItem()).send(new DecimalType(targetState.getCode()));
            } else {
                logger.warn("Item type {} is not supported for {}. Only Number type is supported.",
                        taggedItem.getItem().getType(), taggedItem.getName());
            }
        }, getSubscriber(taggedItem, TARGET_FAN_STATE, updater),
                getUnsubscriber(taggedItem, TARGET_FAN_STATE, updater));
    }

    private static RotationDirectionCharacteristic createRotationDirectionCharacteristic(
            final HomekitTaggedItem taggedItem, HomekitAccessoryUpdater updater) {
        return new RotationDirectionCharacteristic(
                () -> getEnumFromItem(taggedItem, RotationDirectionEnum.CLOCKWISE,
                        RotationDirectionEnum.COUNTER_CLOCKWISE, RotationDirectionEnum.CLOCKWISE),
                (value) -> setValueFromEnum(taggedItem, value, RotationDirectionEnum.CLOCKWISE,
                        RotationDirectionEnum.COUNTER_CLOCKWISE),
                getSubscriber(taggedItem, ROTATION_DIRECTION, updater),
                getUnsubscriber(taggedItem, ROTATION_DIRECTION, updater));
    }

    private static SwingModeCharacteristic createSwingModeCharacteristic(final HomekitTaggedItem taggedItem,
            HomekitAccessoryUpdater updater) {
        return new SwingModeCharacteristic(
                () -> getEnumFromItem(taggedItem, SwingModeEnum.SWING_DISABLED, SwingModeEnum.SWING_ENABLED,
                        SwingModeEnum.SWING_DISABLED),
                (value) -> setValueFromEnum(taggedItem, value, SwingModeEnum.SWING_DISABLED,
                        SwingModeEnum.SWING_ENABLED),
                getSubscriber(taggedItem, SWING_MODE, updater), getUnsubscriber(taggedItem, SWING_MODE, updater));
    }

    private static LockPhysicalControlsCharacteristic createLockPhysicalControlsCharacteristic(
            final HomekitTaggedItem taggedItem, HomekitAccessoryUpdater updater) {
        return new LockPhysicalControlsCharacteristic(
                () -> getEnumFromItem(taggedItem, LockPhysicalControlsEnum.CONTROL_LOCK_DISABLED,
                        LockPhysicalControlsEnum.CONTROL_LOCK_ENABLED, LockPhysicalControlsEnum.CONTROL_LOCK_DISABLED),
                (value) -> setValueFromEnum(taggedItem, value, LockPhysicalControlsEnum.CONTROL_LOCK_DISABLED,
                        LockPhysicalControlsEnum.CONTROL_LOCK_ENABLED),
                getSubscriber(taggedItem, LOCK_CONTROL, updater), getUnsubscriber(taggedItem, LOCK_CONTROL, updater));
    }

    private static RotationSpeedCharacteristic createRotationSpeedCharacteristic(final HomekitTaggedItem item,
            HomekitAccessoryUpdater updater) {
        return new RotationSpeedCharacteristic(getIntSupplier(item), setIntConsumer(item),
                getSubscriber(item, ROTATION_SPEED, updater), getUnsubscriber(item, ROTATION_SPEED, updater));
    }

    private static SetDurationCharacteristic createDurationCharacteristic(final HomekitTaggedItem taggedItem,
            HomekitAccessoryUpdater updater) {
        return new SetDurationCharacteristic(() -> {
            int value = getIntFromItem(taggedItem);
            if (value == 0) { // check for default duration
                final Object duration = taggedItem.getConfiguration().get(HomekitValveImpl.CONFIG_DEFAULT_DURATION);
                if (duration instanceof BigDecimal) {
                    value = ((BigDecimal) duration).intValue();
                    if (taggedItem.getItem() instanceof NumberItem) {
                        ((NumberItem) taggedItem.getItem()).setState(new DecimalType(value));
                    }
                }
            }
            return CompletableFuture.completedFuture(value);
        }, setIntConsumer(taggedItem), getSubscriber(taggedItem, DURATION, updater),
                getUnsubscriber(taggedItem, DURATION, updater));
    }

    private static RemainingDurationCharacteristic createRemainingDurationCharacteristic(
            final HomekitTaggedItem taggedItem, HomekitAccessoryUpdater updater) {
        return new RemainingDurationCharacteristic(getIntSupplier(taggedItem),
                getSubscriber(taggedItem, REMAINING_DURATION, updater),
                getUnsubscriber(taggedItem, REMAINING_DURATION, updater));
    }

    private static VolumeCharacteristic createVolumeCharacteristic(final HomekitTaggedItem taggedItem,
            HomekitAccessoryUpdater updater) {
        return new VolumeCharacteristic(getIntSupplier(taggedItem),
                (volume) -> ((NumberItem) taggedItem.getItem()).send(new DecimalType(volume)),
                getSubscriber(taggedItem, DURATION, updater), getUnsubscriber(taggedItem, DURATION, updater));
    }
}
