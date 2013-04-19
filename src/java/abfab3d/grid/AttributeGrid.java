package abfab3d.grid;

/**
 * A grid that can contain per-voxel attributes.
 *
 * @author Alan Hudson
 */
public interface AttributeGrid extends Grid {
    /**
     * Get the state of the voxel
     *
     * @param x The x world coordinate
     * @param y The y world coordinate
     * @param z The z world coordinate
     * @return The voxel attribute
     */
    public int getAttribute(double x, double y, double z);

    /**
     * Get the attribute of the voxel.
     *
     * @param x The x grid coordinate
     * @param y The y grid coordinate
     * @param z The z grid coordinate
     * @return The voxel attribute
     */
    public int getAttribute(int x, int y, int z);

    /**
     * Set the value of a voxel.
     *
     * @param x The x world coordinate
     * @param y The y world coordinate
     * @param z The z world coordinate
     * @param state The value.  0 = nothing. > 0 attributeID
     * @param attribute The attributeID
     */
    public void setData(double x, double y, double z, byte state, int attribute);

    /**
     * Set the value of a voxel.
     *
     * @param x The x world coordinate
     * @param y The y world coordinate
     * @param z The z world coordinate
     * @param state The value.  0 = nothing. > 0 attributeID
     * @param attribute The attributeID
     */
    public void setData(int x, int y, int z, byte state, int attribute);

    /**
     * Set the attribute value of a voxel.  Leaves the state unchanged.
     *
     * @param x The x world coordinate
     * @param y The y world coordinate
     * @param z The z world coordinate
     * @param attribute The attributeID
     */
    public void setAttribute(int x, int y, int z, int attribute);

    /**
     * Count a class of attribute types.  May be much faster then
     * full grid traversal for some implementations.
     *
     * @param mat The class of attribute to traverse
     * @return The number
     */
    public int findCount(int mat);

    /**
     * Count a class of voxels types.  May be much faster then
     * full grid traversal for some implementations.
     *
     * @param vc The class of voxels to traverse
     * @param t The traverer to call for each voxel
     */
    public void findAttribute(VoxelClasses vc, ClassAttributeTraverser t);

    /**
     * Traverse a class of voxels types.  May be much faster then
     * full grid traversal for some implementations.
     *
     * @param vc The class of voxels to traverse
     * @param t The traverer to call for each voxel
     */
    public void findAttributeInterruptible(VoxelClasses vc, ClassAttributeTraverser t);

    /**
     * Traverse a class of voxels types.  May be much faster then
     * full grid traversal for some implementations.
     *
     * @param mat The attribute to traverse
     * @param t The traverer to call for each voxel
     */
    public void findAttribute(int mat, ClassAttributeTraverser t);

    /*
     * Traverse a class of voxel and attribute types.  May be much faster then
     * full grid traversal for some implementations.  Allows interruption
     * of the find stream.
     *
     * @param vc The class of voxels to traverse
     * @param mat The attribute to traverse
     * @param t The traverer to call for each voxel
     */
    public void findAttribute(VoxelClasses vc, int mat, ClassAttributeTraverser t);

    /**
     * Traverse a class of voxels types.  May be much faster then
     * full grid traversal for some implementations.  Allows interruption
     * of the find stream.
     *
     * @param mat The attribute to traverse
     * @param t The traverer to call for each voxel
     */
    public void findAttributeInterruptible(int mat, ClassAttributeTraverser t);

    /*
     * Traverse a class of voxel and attribute types.  May be much faster then
     * full grid traversal for some implementations.  Allows interruption
     * of the find stream.
     *
     * @param vc The class of voxels to traverse
     * @param mat The attribute to traverse
     * @param t The traverer to call for each voxel
     */
    public void findAttributeInterruptible(VoxelClasses vc, int mat, ClassAttributeTraverser t);

    /**
     * Remove all voxels associated with the Attribute.
     *
     * @param mat The attributeID
     */
    public void removeAttribute(int mat);

    /**
     * Reassign a group of attributes to a new attributeID
     *
     * @param attributes The new list of attributes
     * @param matID The new attributeID
     */
    public void reassignAttribute(int[] attributes, int matID);

}
