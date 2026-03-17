package com.github.cinnaio.natureEngine.api;

import com.github.cinnaio.natureEngine.bootstrap.ServiceLocator;
import com.github.cinnaio.natureEngine.core.agriculture.crop.CropType;
import com.github.cinnaio.natureEngine.core.agriculture.crop.CropManager;
import org.bukkit.Location;

import java.util.Optional;

public final class CropAPI {

    private static final ServiceLocator SERVICES = ServiceLocator.getInstance();

    private CropAPI() {
    }

    public static Optional<CropType> getCropData(Location location) {
        return SERVICES.get(CropManager.class).getCropDataForLocation(location);
    }
}

