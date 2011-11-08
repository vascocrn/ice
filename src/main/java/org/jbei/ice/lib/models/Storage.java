package org.jbei.ice.lib.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.Cascade;
import org.jbei.ice.lib.dao.IModel;

/**
 * Store sample storage location information as well as the hierarchical structure information.
 * <p>
 * This class performs two functions. The reasoning will be explained below.
 * <p>
 * First, this class stores the location information, identified by the name and index fields. For
 * example, a 96 well plate numbered 42 may have name="Plate" and index="42". The plate may have
 * children, say 96 of them. This class does not restrict the number of children, but a controller
 * may look at this class's {@link StorageType} field and determine the minimum and maximum number
 * of children this storage unit can have.
 * <p>
 * The children of Plate-42 will be 96 wells, with name="Well" and index numbered from "A1" to
 * "H12". When a {@link Sample} is associated with a location, it may in this case point to the
 * Storage object Well-A1. In turn, Well-A1 is held in Plate-42. Conveniently, the name and index
 * can be concatenated to give the user a friendly name, such as Plate-42 and Well-A1.
 * <p>
 * There is not a limit on the amount of hierarchy a Storage object can have. For example, Freezer-1
 * can hold Shelf-2 which holds Rack-3 which holds Plate-42 which holds Well-A1. In a large freezer,
 * there maybe many Well-A1s, but they all have unique ids (UUIDs) and different parents. The
 * associations can be freely moved around by changing the parent field. For example, Plate-42 maybe
 * moved to Rack-1 from Rack-3, and the children wells would not notice.
 * <p>
 * Furthermore, there is no limitation on what can be a child of a parent. In a real freezer,
 * Shelf-2 may contain Rack-3 and also a paper box with 81 slots (Call them Box-5, wth Slot-1 to
 * Slot-81), with a tube in each slot (Tube-1138). In this case, a Sample object would be associated
 * with Tube-1138, as that is the object that can be moved between slots or boxes.
 * <p>
 * Most laboratories have a fixed types of containers, or some scheme of organization. So it would
 * be useful to store some information about the scheme of hierarchy such as
 * Freezer->Shelf->Rack->Plate->Well or Freezer->Shelf->Box->Slot->Tube. Additionally, different
 * entry types may require different storage schemes. For example, plasmids can be stored in tubes
 * or in plates, and seeds can be stored in tubes or boxes.
 * <p>
 * Therefore, what is needed is also a class for storage schemes with its own parent/child
 * relationships. For example, Plasmid storage scheme can have two child storage schemes, one for
 * plates and other for tubes. But the difficulty lies in that each storage object can have multiple
 * child storage schemes. For example, the shelf of a freezer can contain both racks and boxes, with
 * separate storage schemes. So we work around this by inserting the storage schemes directly into
 * the storage object instead of creating a separate class. Furthermore, if the storage scheme
 * changes, for example insertion or deletion of layers, this does not adversely affect already
 * existing storage hierarchies.
 * <p>
 * There are other advantages of storing the scheme information directly into the Storage object.
 * And then there are disadvantages. Advantages seem to outweigh the disadvantages in this
 * application. <b>TODO</b>.
 * 
 * @author Timothy Ham, Hector Plahar
 * 
 */
@Entity
@Table(name = "storage")
@SequenceGenerator(name = "sequence", sequenceName = "storage_id_seq", allocationSize = 1)
public class Storage implements IModel {

    private static final long serialVersionUID = 1L;

    public enum StorageType {
        GENERIC, FREEZER, SHELF, BOX_INDEXED, BOX_UNINDEXED, PLATE96, WELL, TUBE, SCHEME
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence")
    private long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_id")
    private Storage parent;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "index", length = 31, nullable = true)
    private String index;

    @Column(name = "description", length = 1023)
    private String description;

    @Column(name = "uuid", length = 36, nullable = false)
    private String uuid;

    @Column(name = "storage_type")
    @Enumerated(EnumType.STRING)
    private StorageType storageType;

    @Column(name = "owner_email", length = 255, nullable = false)
    private String ownerEmail;

    @Column(name = "schemes")
    @Lob
    private ArrayList<Storage> schemes;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "parent")
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    @OrderBy("id")
    @JoinColumn(name = "parent_id")
    private final Set<Storage> children = null;

    public Storage() {
        super();
    }

    public Storage(String name, String description, StorageType storageType, String ownerEmail,
            Storage parent) {
        setName(name);
        setDescription(description);
        setStorageType(storageType);
        setOwnerEmail(ownerEmail);
        setParent(parent);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Storage getParent() {
        return parent;
    }

    public void setParent(Storage parent) {
        this.parent = parent;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public StorageType getStorageType() {
        return storageType;
    }

    public void setStorageType(StorageType storageType) {
        this.storageType = storageType;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    /**
     * Make sure to get a copy from database before calling this method
     * 
     * @return
     */
    public Set<Storage> getChildren() {
        return children;
    }

    public static Map<String, String> getLocationTypeOptionsMap() {
        Map<String, String> result = new HashMap<String, String>();

        result.put(StorageType.GENERIC.toString(), "Generic");
        result.put(StorageType.FREEZER.toString(), "Freezer");
        result.put(StorageType.SHELF.toString(), "Shelf");
        result.put(StorageType.BOX_INDEXED.toString(), "Indexed Box");
        result.put(StorageType.BOX_UNINDEXED.toString(), "Unindexed Box");
        result.put(StorageType.PLATE96.toString(), "96 Well Plate");
        result.put(StorageType.WELL.toString(), "Numbered Well");
        result.put(StorageType.TUBE.toString(), "Labeled Tube");

        return result;
    }

    public void setSchemes(ArrayList<Storage> schemes) {
        this.schemes = schemes;
    }

    public ArrayList<Storage> getSchemes() {
        return schemes;
    }

    @Override
    public String toString() {
        if (getStorageType().equals(StorageType.SCHEME)) {
            return getName();
        } else {
            return getName() + " " + getIndex();
        }
    }
}
