package com.gradualgames.ggvm;

import java.util.List;

/**
 * Created by derek on 8/24/2016.
 *
 * This interface provides a list of ReadWriteRange components that can
 * be installed on a Bus memory map. Mappers will be ReadWriteRangeProviders.
 * Other components such as the Ppu which provide several registers at different
 * locations on the bus will also be ReadWriteRangeProviders.
 */
public interface ReadWriteRangeProvider {

    List<ReadWriteRange> provideReadWriteRanges(Bus.BusType busType);

}
