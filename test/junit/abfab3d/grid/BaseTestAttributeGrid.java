/*****************************************************************************
 *                        Shapeways, Inc Copyright (c) 2011
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package abfab3d.grid;

// External Imports
import abfab3d.io.output.BoxesX3DExporter;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.web3d.util.ErrorReporter;
import org.web3d.vrml.export.PlainTextErrorReporter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

// Internal Imports

/**
 * Base functionality for testing grids.  Only uses the Grid interface.
 *
 * @author Alan Hudson
 * @version
 */
public class BaseTestAttributeGrid extends TestCase {
    /**
     * Set and get all values of a grid using voxel coords
     *
     * @param grid The grid to test
     */
    public void setGetAllVoxelCoords(AttributeGrid grid) {
        int width = grid.getWidth();
        int height = grid.getHeight();
        int depth = grid.getDepth();

        for(int y=0; y < height; y++) {
            for(int x=0; x < width; x++) {
                for(int z=0; z < depth; z++) {
                    grid.setData(x,y,z,Grid.EXTERIOR, 1);
                }
            }
        }

        VoxelData vd = grid.getVoxelData();

        for(int y=0; y < height; y++) {
            for(int x=0; x < width; x++) {
                for(int z=0; z < depth; z++) {
                    grid.getData(x,y,z,vd);
                    assertTrue("State wrong", vd.getState() == Grid.EXTERIOR);
                    assertTrue("Material wrong", vd.getMaterial() == 1);
                }
            }
        }
    }

    /**
     * Set and get all values of a grid using voxel coords using stripped
     * exterior/interior pattern.
     *
     * @param grid The grid to test
     */
    public void setGetAllVoxelCoordsStripped(AttributeGrid grid) {
        int width = grid.getWidth();
        int height = grid.getHeight();
        int depth = grid.getDepth();

        for(int y=0; y < height; y++) {
            for(int x=0; x < width; x++) {
                for(int z=0; z < depth; z++) {
                    if ((x % 2) == 0 && (y % 2) == 0 && (z % 2) == 0) {
                        grid.setData(x,y,z,Grid.EXTERIOR, 1);
                    } else {
                        grid.setData(x,y,z,Grid.INTERIOR, 2);
                    }

                }
            }
        }

        VoxelData vd = grid.getVoxelData();
        for(int y=0; y < height; y++) {
            for(int x=0; x < width; x++) {
                for(int z=0; z < depth; z++) {
                    grid.getData(x,y,z,vd);
//System.out.println(x + ", " + y + ", " + z + ": " + vd.getState());
                    if ((x % 2) == 0 && (y % 2) == 0 && (z % 2) == 0) {
                        assertTrue("State wrong", vd.getState() == Grid.EXTERIOR);
                        assertTrue("Material wrong", vd.getMaterial() == 1);
                    } else {
                        assertTrue("State wrong", vd.getState() == Grid.INTERIOR);
                        assertTrue("Material wrong", vd.getMaterial() == 2);
                    }
                }
            }
        }
    }

    /**
     * Set and get all values of a grid using voxel coords using stripped
     * exterior/interior pattern.
     *
     * @param grid The grid to test
     */
    public void setGetAllVoxelCoordsDiagonal(AttributeGrid grid) {
        int width = grid.getWidth();
        int height = grid.getHeight();
        int depth = grid.getDepth();

        for(int y=0; y < height; y++) {
            for(int x=0; x < width; x++) {
                for(int z=0; z < depth; z++) {
                    if (x == y && y == z) {
                        grid.setData(x,y,z,Grid.EXTERIOR, 1);
                    }
                }
            }
        }

        VoxelData vd = grid.getVoxelData();
        for(int y=0; y < height; y++) {
            for(int x=0; x < width; x++) {
                for(int z=0; z < depth; z++) {
                    grid.getData(x,y,z,vd);
//System.out.println(x + ", " + y + ", " + z + ": " + vd.getState());
                    if (x == y && y == z) {
                        assertTrue("State wrong", vd.getState() == Grid.EXTERIOR);
                        assertTrue("Material wrong", vd.getMaterial() == 1);
                    }
                }
            }
        }
    }

    /**
     * Set and get all values of a grid using world coords
     *
     * @param grid The grid to test
     */
    public void setGetAllVoxelByWorldCoords(AttributeGrid grid) {
        int width = grid.getWidth();
        int height = grid.getHeight();
        int depth = grid.getDepth();
        double voxelSize = grid.getVoxelSize();
        double sliceHeight = grid.getSliceHeight();

        double xcoord, ycoord, zcoord;

        for(int x=0; x < width; x++) {
            xcoord = (double)(x)*voxelSize + voxelSize/2.0;
            for(int y=0; y < height; y++) {
                ycoord = (double)(y)*sliceHeight + sliceHeight/2.0;
                for(int z=0; z < depth; z++) {
                    zcoord = (double)(z)*voxelSize + voxelSize/2.0;
                    grid.setData(xcoord, ycoord, zcoord, Grid.EXTERIOR, (byte)1);
                }
            }
        }

        VoxelData vd = grid.getVoxelData();

        for(int x=0; x < width; x++) {
            xcoord = (double)(x)*voxelSize + voxelSize/2.0;
            for(int y=0; y < height; y++) {
                ycoord = (double)(y)*sliceHeight + sliceHeight/2.0;
                for(int z=0; z < depth; z++) {
                    zcoord = (double)(z)*voxelSize + voxelSize/2.0;
                    grid.getData(xcoord, ycoord, zcoord,vd);
//System.out.println(x + ", " + y + ", " + z + ": " + vd.getState());
                    assertTrue("State wrong", vd.getState() == Grid.EXTERIOR);
                    assertTrue("Material wrong", vd.getMaterial() == 1);
                }
            }
        }
    }

    /**
     * Set the X values of a grid with a given Y and Z to the given state and material.
     *
     * @param state The new state
     * @param mat The new material
     * @param startIndex The starting X index
     * @param endIndex The ending X Index
     */
    protected static void setX(AttributeGrid grid, int y, int z, byte state, int mat, int startIndex, int endIndex) {
        for(int x=startIndex; x <= endIndex; x++) {
            grid.setData(x,y,z, state, mat);
        }
    }

    /**
     * Set the Y values of a grid with a given X and Z to the given state and material.
     *
     * @param state The new state
     * @param mat The new material
     * @param startIndex The starting Y index
     * @param endIndex The ending Y Index
     */
    protected static void setY(AttributeGrid grid, int x, int z, byte state, int mat, int startIndex, int endIndex) {
        for(int y=startIndex; y <= endIndex; y++) {
            grid.setData(x,y,z, state, mat);
        }
    }

    /**
     * Set the Z values of a grid with a given X and Y to the given state and material.
     *
     * @param state The new state
     * @param mat The new material
     * @param startIndex The starting Z index
     * @param endIndex The ending Z Index
     */
    protected static void setZ(AttributeGrid grid, int x, int y, byte state, int mat, int startIndex, int endIndex) {
        for(int z=startIndex; z <= endIndex; z++) {
            grid.setData(x,y,z, state, mat);
        }
    }


    /**
     * Set the data for an X plane.
     *
     * @param grid The grid to set
     * @param x The X plane to set
     * @param state The new state
     * @param material The new material
     */
    protected static void setPlaneX(AttributeGrid grid, int x, byte state, int material) {
        int height = grid.getHeight();
        int depth = grid.getDepth();

        for (int y=0; y<height; y++) {
            for (int z=0; z<depth; z++) {
                grid.setData(x, y, z, state, material);
            }
        }
    }

    /**
     * Set the data for a Y plane.
     *
     * @param grid The grid to set
     * @param y The Y plane to set
     * @param state The new state
     * @param material The new material
     */
    protected static void setPlaneY(AttributeGrid grid, int y, byte state, int material) {
        int width = grid.getWidth();
        int depth = grid.getDepth();

        for (int x=0; x<width; x++) {
            for (int z=0; z<depth; z++) {
                grid.setData(x, y, z, state, material);
            }
        }
    }

    /**
     * Set the data for a Z plane.
     *
     * @param grid The grid to set
     * @param z The Z plane to set
     * @param state The new state
     * @param material The new material
     */
    protected static void setPlaneZ(AttributeGrid grid, int z, byte state, int material) {
        int width = grid.getWidth();
        int height = grid.getHeight();

        for (int x=0; x<width; x++) {
            for (int y=0; y<height; y++) {
                grid.setData(x, y, z, state, material);
            }
        }
    }

    protected static void saveDebug(Grid grid, String filename, boolean showOutside) {
        ErrorReporter console = new PlainTextErrorReporter();

        try {
            FileOutputStream fos = new FileOutputStream(filename);
            String encoding = filename.substring(filename.lastIndexOf(".") + 1);
            BoxesX3DExporter exporter = new BoxesX3DExporter(encoding, fos, console);

            HashMap<Integer, float[]> colors = new HashMap<Integer, float[]>();
            colors.put(new Integer(Grid.INTERIOR), new float[]{0, 1, 0});
            colors.put(new Integer(Grid.EXTERIOR), new float[]{1, 0, 0});

            HashMap<Integer, Float> transparency = new HashMap<Integer, Float>();
            transparency.put(new Integer(Grid.INTERIOR), new Float(0));
            transparency.put(new Integer(Grid.EXTERIOR), new Float(0.5));
            if (showOutside) {
                colors.put(new Integer(Grid.OUTSIDE), new float[]{0, 0, 1});
                transparency.put(new Integer(Grid.OUTSIDE), new Float(0.96));
            }


            exporter.writeDebug(grid, colors, transparency);
            exporter.close();

            fos.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

    }
}
