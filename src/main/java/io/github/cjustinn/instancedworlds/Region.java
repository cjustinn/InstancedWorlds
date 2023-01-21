package io.github.cjustinn.instancedworlds;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

public class Region {

    private Location cornerOne;
    private Location cornerTwo;

    public Region(Location c1, Location c2) {
        this.cornerOne = c1;
        this.cornerTwo = c2;
    }

    public Location getCornerOne() { return this.cornerOne; }
    public Location getCornerTwo() { return this.cornerTwo; }

    public boolean contains(Location pos) {
        boolean isInRegion = true;
        boolean cornerOneIsLarger = false;

        // Check X
        cornerOneIsLarger = cornerOne.getBlockX() > cornerTwo.getBlockX();
        if (cornerOneIsLarger) {

            if (Math.floor(pos.getX()) > cornerOne.getBlockX() || Math.floor(pos.getX()) < cornerTwo.getBlockX())
                isInRegion = false;

        } else {

            if (Math.floor(pos.getX()) < cornerOne.getBlockX() || Math.floor(pos.getX()) > cornerTwo.getBlockX())
                isInRegion = false;

        }

        // Check Z
        cornerOneIsLarger = cornerOne.getBlockZ() > cornerTwo.getBlockZ();
        if (cornerOneIsLarger && isInRegion) {

            if (Math.floor(pos.getZ()) > cornerOne.getBlockZ() || Math.floor(pos.getZ()) < cornerTwo.getBlockZ())
                isInRegion = false;

        } else {

            if (Math.floor(pos.getZ()) < cornerOne.getBlockZ() || Math.floor(pos.getZ()) > cornerTwo.getBlockZ())
                isInRegion = false;

        }

        // Check Y
        cornerOneIsLarger = cornerOne.getBlockY() > cornerTwo.getBlockY();
        if (cornerOneIsLarger && isInRegion) {

            if (Math.floor(pos.getY()) > cornerOne.getBlockY() || Math.floor(pos.getY()) < cornerTwo.getBlockY())
                isInRegion = false;

        } else {

            if (Math.floor(pos.getY()) < cornerOne.getBlockY() || Math.floor(pos.getY()) > cornerTwo.getBlockY())
                isInRegion = false;

        }

        // Return the boolean flag.
        return isInRegion;
    }

}
