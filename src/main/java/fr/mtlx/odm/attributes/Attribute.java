package fr.mtlx.odm.attributes;

import java.io.Serializable;

public interface Attribute extends Serializable {
	String getOID();

	String getSyntax();
}
