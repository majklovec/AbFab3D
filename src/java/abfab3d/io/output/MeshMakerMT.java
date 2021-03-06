/*****************************************************************************
 *                        Shapeways, Inc Copyright (c) 2012
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/
package abfab3d.io.output;

import abfab3d.grid.Grid;
import abfab3d.mesh.EdgeTester;
import abfab3d.mesh.IndexedTriangleSetBuilder;
import abfab3d.mesh.MeshDecimator;
import abfab3d.mesh.WingedEdgeTriangleMesh;
import abfab3d.util.MathUtil;
import abfab3d.util.TriangleCollector;

import javax.vecmath.Vector3d;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static abfab3d.util.Output.printf;
import static abfab3d.util.Output.time;
import static java.lang.System.nanoTime;

/**
 * takes grid extract isosurface and decimates mesh in one operation.
 * <p/>
 * Uses multihreaded pipeline by convertig grid into array of small blocks, which fit into processor cache
 *
 * @author Vladimir Bulatov
 */
public class MeshMakerMT {
    /** Should we print debug information */
    private static final boolean DEBUG = false;

    /** Should we collect stats information */
    private static final boolean STATS = false;

    static final double MM = 0.001;

    public static final int RESULT_OK = 0;

    protected int m_threadCount = 1;

    // size of block of grid to make in one chunk 
    protected int m_blockSize = 20;
    protected int m_noDecimationSize = 100;

    //max error of decimation
    protected double m_maxDecimationError = 1.e-9;

    protected int m_interpolationAlgorithm = IsosurfaceMaker.INTERPOLATION_LINEAR;

    protected double m_smoothingWidth = 1.;
    protected int m_gridMaxAttributeValue = 0;
    protected int m_maxDecimationCount = 7;

    // Maximum allowed triangles.  Will relax maxDecimationError to achieve
    protected int m_maxTriangles = Integer.MAX_VALUE;
    protected EdgeTester m_edgeTester;

    public MeshMakerMT() {

    }

    public void setThreadCount(int count) {

        if (count < 1)
            count = 1;

        this.m_threadCount = count;

    }

    public void setMaxTriangles(int tris) {
        this.m_maxTriangles = tris;
    }

    public void setMaxAttributeValue(int gridMaxAttributeValue) {

        m_gridMaxAttributeValue = gridMaxAttributeValue;

    }

    public void setMaxDecimationError(double value) {

        m_maxDecimationError = value;

    }

    public void setSmoothingWidth(double value) {

        m_smoothingWidth = value;

    }


    public void setBlockSize(int size) {
        m_blockSize = size;
    }

    public void setMaxDecimationCount(int count) {

        m_maxDecimationCount = count;

    }

    /**
     * set tester to test edge collapses
     * edge can be collapsed only if tester return true
     */
    public void setEdgeTester(EdgeTester tester) {

        m_edgeTester = tester;

    }


    /**
       set interpolation algorith to use 
       INTERPOLATION_LINEAR
       or 
       INTERPOLATION_INDICATOR_FUNCTION       
     */
    public void setInterpolationAlgorithm(int algorithm){

        m_interpolationAlgorithm = algorithm;
    }

    /**
     * creates mesh and feeds it into triangle collector
     */
    public int makeMesh(Grid grid, TriangleCollector tc) {

        long t0 = time();

        GridBlockSet blocks = makeBlocks(grid.getWidth() - 1, grid.getHeight() - 1, grid.getDepth() - 1, m_blockSize);


        ExecutorService executor = Executors.newFixedThreadPool(m_threadCount);

        BlockProcessor threads[] = new BlockProcessor[m_threadCount];
        double smoothKernel[] = null;
        if (m_smoothingWidth > 0.) {
            smoothKernel = MathUtil.getGaussianKernel(m_smoothingWidth);
        }

        for (int i = 0; i < m_threadCount; i++) {
            threads[i] = new BlockProcessor(grid, blocks, m_maxDecimationError, smoothKernel, m_gridMaxAttributeValue);
            if (m_edgeTester != null) {
                threads[i].setEdgeTester((EdgeTester) (m_edgeTester.clone()));
            }
            executor.submit(threads[i]);

        }

        executor.shutdown();

        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }

        long num_tris = 0;
        for(int i=0; i < threads.length;i++) {
            num_tris += threads[i].getNumTriangles();
        }

        printf("Raw number of triangles before decimation: %d\n",num_tris);


        printf("MESH_EXTRACTION_TIME: %d ms\n", (time() - t0));

        blocks.rewind();
        GridBlock block;
        int origFaceCount = 0, finalFaceCount = 0;

        while ((block = blocks.getNext()) != null) {

            //printf(" isosurface: %d ms, decimation: %d ms  faces: %d -> : %d\n",
            //       block.timeIsosurface, block.timeDecimation, block.origFaceCount,block.finalFaceCount);
            origFaceCount += block.origFaceCount;
            finalFaceCount += block.finalFaceCount;
        }

        printf("finalFaceCount: %d\n", finalFaceCount);

        int max_loop = 3;
        int attempts = 0;
        int last_tri_count =0;

        while(finalFaceCount > m_maxTriangles && attempts < max_loop) {
            blocks.rewind();
            // TODO: Not sure how aggressive to change decimation error;
            double max_decimation_error = m_maxDecimationError * Math.pow(10.0,attempts + 1);
            System.out.println("Count is above max triangle limit: " + finalFaceCount + " new decimationError: " + max_decimation_error);

            executor = Executors.newFixedThreadPool(m_threadCount);

            BlockDecimator[] workers = new BlockDecimator[m_threadCount];
            for (int i = 0; i < m_threadCount; i++) {
                workers[i] = new BlockDecimator(blocks, max_decimation_error);
                if (m_edgeTester != null) {
                    workers[i].setEdgeTester((EdgeTester) (m_edgeTester.clone()));
                }

                executor.submit(workers[i]);
            }

            executor.shutdown();

            try {
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (Exception e) {
                e.printStackTrace();
            }

            blocks.rewind();

            finalFaceCount = 0;
            while ((block = blocks.getNext()) != null) {

                //printf(" isosurface: %d ms, decimation: %d ms  faces: %d -> : %d\n",
                //       block.timeIsosurface, block.timeDecimation, block.origFaceCount,block.finalFaceCount);
                finalFaceCount += block.finalFaceCount;
            }

            System.out.println("Current faceCount: " + finalFaceCount);
            attempts++;

            if (finalFaceCount == last_tri_count) {
                System.out.println("No new triangles found, bailing");
                break;
            }

            last_tri_count = finalFaceCount;
        }

        blocks.rewind();
        origFaceCount = 0;
        finalFaceCount = 0;

        while ((block = blocks.getNext()) != null) {

            //printf(" isosurface: %d ms, decimation: %d ms  faces: %d -> : %d\n",
            //       block.timeIsosurface, block.timeDecimation, block.origFaceCount,block.finalFaceCount);
            origFaceCount += block.origFaceCount;
            finalFaceCount += block.finalFaceCount;
            block.writeTriangles(tc);

        }

        printf("originalFaceCount: %d\n", origFaceCount);
        printf("finalFaceCount: %d\n", finalFaceCount);

        return RESULT_OK;

    }

    /**
     * generates set of blocks of approximately blockSize, which tile the (NX x NY x NZ) grid
     * if tiles
     */
    GridBlockSet makeBlocks(int nx, int ny, int nz, int blockSize) {

        int blocksX = (nx + blockSize - 1) / blockSize;
        int blocksY = (ny + blockSize - 1) / blockSize;
        int blocksZ = (nz + blockSize - 1) / blockSize;
        // minial sizes of blocks 
        int bx = nx / blocksX;
        int by = ny / blocksY;
        int bz = nz / blocksZ;

        // reminders. We add 1 to size of first rx blocks in eac directions 
        int rx = nx % blocksX;
        int ry = ny % blocksY;
        int rz = nz % blocksZ;

        GridBlockSet blocks = new GridBlockSet();

        for (int y = 0, iy = 0; y < blocksY; y++) {

            int iy1 = iy + by;
            if (y < ry) iy1++;

            for (int x = 0, ix = 0; x < blocksX; x++) {

                int ix1 = ix + bx;
                if (x < rx) ix1++;

                for (int z = 0, iz = 0; z < blocksZ; z++) {

                    int iz1 = iz + bz;
                    if (z < rz) iz1++;
                    GridBlock block = new GridBlock();
                    block.setBlock(ix, ix1, iy, iy1, iz, iz1);
                    //if(DEBUG) printf("block: (%3d,%3d,%3d,%3d,%3d,%3d)\n", ix, ix1, iy, iy1, iz, iz1);

                    blocks.add(block);
                    iz = iz1;
                }
                ix = ix1;
            }
            iy = iy1;
        }

        System.out.println("***Total blocks made: " + blocks.gridBlocks.size() + " min tris is: " + (blocks.gridBlocks.size() * 100));
        return blocks;

    } // makeBlocks     


    /**
     * block of grid
     */
    static class GridBlock {

        // mesh generated 
        //WingedEdgeTriangleMesh mesh; 
        // bounds of the grid fragment 
        //double bounds[];
        int faces[];
        double vertices[];
        // grid dimensions of the grid fragment 
        //int nx, ny, nz;

        // block of grid to process (inclusive) 
        int xmin, xmax, ymin, ymax, zmin, zmax;

        int origFaceCount;
        int finalFaceCount;
        long timeIsosurface;
        long timeDecimation;

        IndexedTriangleSetBuilder its = null;

        void setBlock(int x0, int x1, int y0, int y1, int z0, int z1) {

            xmin = x0;
            xmax = x1;

            ymin = y0;
            ymax = y1;

            zmin = z0;
            zmax = z1;

        }

        void writeTriangles(TriangleCollector tc) {

            if (its != null) {
                its.getTriangles(tc);
                return;
            }

            if (faces == null || faces.length < 3) {
                //printf("triCount: 0\n");
                return;
            }

            // triangles are 
            Vector3d v0 = new Vector3d();
            Vector3d v1 = new Vector3d();
            Vector3d v2 = new Vector3d();

            for (int i = 0; i < faces.length; i += 3) {

                int iv0 = 3 * faces[i];
                int iv1 = 3 * faces[i + 1];
                int iv2 = 3 * faces[i + 2];

                v0.set(vertices[iv0], vertices[iv0 + 1], vertices[iv0 + 2]);
                v1.set(vertices[iv1], vertices[iv1 + 1], vertices[iv1 + 2]);
                v2.set(vertices[iv2], vertices[iv2 + 1], vertices[iv2 + 2]);
                tc.addTri(v0, v1, v2);
            }
        }

    }

    /**
     * collection of grid blocks
     */
    static class GridBlockSet {

        Vector<GridBlock> gridBlocks;
        AtomicInteger currentBlock = new AtomicInteger(0);

        GridBlockSet() {
            gridBlocks = new Vector<GridBlock>();
        }

        public void rewind() {
            currentBlock.set(0);
        }

        public GridBlock getNext() {

            int next = currentBlock.getAndIncrement();
            if (next >= gridBlocks.size())
                return null;
            else
                return gridBlocks.get(next);
        }

        public void add(GridBlock block) {
            gridBlocks.add(block);
        }
    } // class GridBlockSet


    /**
     * extract mesh from a block of the grid
     */
    class BlockProcessor implements Runnable {

        Grid grid;
        // physical bounds of the grid 
        double gridBounds[] = new double[6];
        // physical bounds of the block 
        double blockBounds[] = new double[6];

        GridBlockSet blocks;

        WingedEdgeTriangleMesh mesh;
        IndexedTriangleSetBuilder its;
        double vertices[]; // intermediate memory for vertices 
        int faces[];  // intermediate memory for face indexes 

        int gnx, gny, gnz;
        double gxmin, gymin, gzmin;
        double gdx, gdy, gdz;
        double maxDecimationError;
        IsosurfaceMaker imaker;
        MeshDecimator decimator;
        IsosurfaceMaker.BlockSmoothingSlices slicer;
        double smoothKernel[];
        long origNumTriangles;

        EdgeTester edgeTester;

        BlockProcessor(Grid grid,
                       GridBlockSet blocks,
                       double maxDecimationError,
                       double smoothKernel[],
                       int gridMaxAttributeValue
        ) {

            this.grid = grid;
            this.blocks = blocks;
            this.maxDecimationError = maxDecimationError;
            this.gridBounds = new double[6];
            grid.getGridBounds(gridBounds);

            gnx = grid.getWidth();
            gny = grid.getHeight();
            gnz = grid.getDepth();
            gxmin = gridBounds[0];
            gymin = gridBounds[2];
            gzmin = gridBounds[4];
            gdx = (gridBounds[1] - gridBounds[0]) / gnx;
            gdy = (gridBounds[3] - gridBounds[2]) / gny;
            gdz = (gridBounds[5] - gridBounds[4]) / gnz;

            slicer = new IsosurfaceMaker.BlockSmoothingSlices(grid);
            slicer.setGridMaxAttributeValue(gridMaxAttributeValue);

            this.smoothKernel = smoothKernel;
            //smoothKernel = MathUtil.getBoxKernel(0);

        }

        void setEdgeTester(EdgeTester edgeTester) {

            this.edgeTester = edgeTester;

        }

        public void run() {
            origNumTriangles = 0;
            // make isosurface extrator

            while (true) {

                GridBlock block = blocks.getNext();

                if (block == null)
                    break;

                try {
                    processBlock(block);

                } catch (Exception e) {

                    e.printStackTrace();
                    break;
                }
            }
        }

        void processBlock(GridBlock block) {

            blockBounds[0] = gxmin + block.xmin * gdx + gdx / 2;
            blockBounds[1] = blockBounds[0] + (block.xmax - block.xmin) * gdx;
            blockBounds[2] = gymin + block.ymin * gdy + gdy / 2;
            blockBounds[3] = blockBounds[2] + (block.ymax - block.ymin) * gdy;
            blockBounds[4] = gzmin + block.zmin * gdz + gdz / 2;
            blockBounds[5] = blockBounds[4] + (block.zmax - block.zmin) * gdz;
            if (DEBUG) {
                printf("processBlock() [%6.2f,%6.2f,%6.2f,%6.2f,%6.2f,%6.2f]\n",
                        blockBounds[0] / MM, blockBounds[1] / MM, blockBounds[2] / MM, blockBounds[3] / MM, blockBounds[4] / MM, blockBounds[5] / MM);
            }

            if (imaker == null)
                imaker = new IsosurfaceMaker();

            imaker.setIsovalue(0.);
            imaker.setBounds(blockBounds);
            imaker.setGridSize(block.xmax - block.xmin + 1, block.ymax - block.ymin + 1, block.zmax - block.zmin + 1);
            imaker.setInterpolationAlgorithm(m_interpolationAlgorithm);

            if (its == null) {
                its = new IndexedTriangleSetBuilder();
            } else {
                its.clear();
            }

            long t0;

            if (STATS) {
                t0 = nanoTime();
            }

            slicer.initBlock(block.xmin, block.xmax, block.ymin, block.ymax, block.zmin, block.zmax, smoothKernel);
            if (!slicer.containsIsosurface()) {
                if (DEBUG) System.out.println("Nothing here");
                return;
            }
            imaker.makeIsosurface(slicer, its);

            //imaker.makeIsosurface(new IsosurfaceMaker.SliceGrid2(grid, gridBounds, 2), its);


            //printf("isosurface done %d ms\n", (nanoTime() - t0));

            long t1;
            if (STATS) {
                t1 = nanoTime();

                block.timeIsosurface = (t1 - t0);
            }

            int vertexCount = its.getVertexCount();
            int faceCount = its.getFaceCount();

            origNumTriangles += faceCount;

            //printf("faceCount: %d vertexCount: %d\n", faceCount, vertexCount);

            if (faceCount < m_noDecimationSize) {
                // no decimation is needed 
                block.faces = new int[faceCount * 3];
                its.getFaces(block.faces);
                block.vertices = new double[3 * its.getVertexCount()];
                its.getVertices(block.vertices);

                // TODO: added this, I suspect total faceCount was wrong without this.
                block.finalFaceCount = faceCount;
                return;
            }

            // will do decimation 
            vertices = its.getVertices(vertices);
            faces = its.getFaces(faces);

            block.origFaceCount = faceCount;

            //printf("vertCount: %d faceCont: %d\n", vertexCount, faceCount);        

            if (mesh == null) {
                //printf("new mesh\n");
                mesh = new WingedEdgeTriangleMesh(vertices, vertexCount, faces, faceCount);
            } else {
                //printf("reusing old mesh\n");
                mesh.clear();
                mesh.setFaces(vertices, vertexCount, faces, faceCount);
            }

            //block.mesh = 
            //intf("mesh created: %d ms\n", (time() - t0));

            if (decimator == null) {
                decimator = new MeshDecimator();
                decimator.setMaxCollapseError(maxDecimationError);
                if (edgeTester != null) {
                    decimator.setEdgeTester(edgeTester);
                }
            }

            if (edgeTester != null) {
                edgeTester.initialize(mesh);
            }

            //printf("start decimation\n");

            int count = m_maxDecimationCount;

            int fcount = mesh.getTriangleCount();

            while (count-- > 0) {

                int target = fcount / 2;
                decimator.processMesh(mesh, target);
                fcount = mesh.getTriangleCount();

            }

            //printf("decimation done. fcount: %d\n",fcount);

            if (STATS) block.timeDecimation = (nanoTime() - t1);
            IndexedTriangleSetBuilder its = new IndexedTriangleSetBuilder(fcount);
            mesh.getTriangles(its);
            block.its = its;
            block.finalFaceCount = fcount;

        }

        public long getNumTriangles() {
            return origNumTriangles;
        }

    } // class BlockProcessor

    /**
     * Decimate a block further
     */
    class BlockDecimator implements Runnable {

        GridBlockSet blocks;

        WingedEdgeTriangleMesh mesh;
        double vertices[]; // intermediate memory for vertices
        int faces[];  // intermediate memory for face indexes

        double maxDecimationError;
        MeshDecimator decimator;
        EdgeTester edgeTester;

        BlockDecimator(GridBlockSet blocks,
                       double maxDecimationError
        ) {

            this.blocks = blocks;
            this.maxDecimationError = maxDecimationError;
        }

        void setEdgeTester(EdgeTester edgeTester) {
            this.edgeTester = edgeTester;
        }

        public void run() {
            while (true) {

                GridBlock block = blocks.getNext();

                if (block == null)
                    break;

                try {
                    processBlock(block);

                } catch (Exception e) {

                    e.printStackTrace();
                    break;
                }
            }
        }

        void processBlock(GridBlock block) {
            long t1 = nanoTime();

            if (block.its == null) {
                return;
            }

            if (block.finalFaceCount < m_noDecimationSize) {
                // no decimation is needed
                return;
            }


            vertices = block.its.getVertices(vertices);
            faces = block.its.getFaces(faces);

            block.origFaceCount = block.finalFaceCount;

            int vertexCount = vertices.length / 3;
            int faceCount = block.finalFaceCount;

            //printf("decimate faceCount: %d vertexCount: %d\n", faceCount, vertexCount);

            //printf("vertCount: %d faceCont: %d\n", vertexCount, faceCount);

            if (mesh == null) {
                //printf("new mesh\n");
                mesh = new WingedEdgeTriangleMesh(vertices, vertexCount, faces, faceCount);
            } else {
                //printf("reusing old mesh\n");
                mesh.clear();
                mesh.setFaces(vertices, vertexCount, faces, faceCount);
            }

            //block.mesh =
            //intf("mesh created: %d ms\n", (time() - t0));

            if (decimator == null) {
                decimator = new MeshDecimator();
                decimator.setMaxCollapseError(maxDecimationError);
                if (edgeTester != null) {
                    decimator.setEdgeTester(edgeTester);
                }
            }

            if (edgeTester != null) {
                edgeTester.initialize(mesh);
            }


            int count = m_maxDecimationCount;

            int fcount = mesh.getTriangleCount();

            while (count-- > 0) {

                int target = fcount / 2;
                decimator.processMesh(mesh, target);
                fcount = mesh.getTriangleCount();

            }

            //printf("decimation done. orig: %d --> fcount: %d\n",block.origFaceCount,fcount);

            if (STATS) block.timeDecimation = (nanoTime() - t1);
            IndexedTriangleSetBuilder its = new IndexedTriangleSetBuilder(fcount);
            mesh.getTriangles(its);
            block.its = its;
            block.finalFaceCount = fcount;
        }

    } // class BlockProcessor

}// class MeshMakerMT 

