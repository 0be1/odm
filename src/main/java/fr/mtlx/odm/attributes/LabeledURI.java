package fr.mtlx.odm.attributes;

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.URI;

import javax.annotation.Nullable;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.Ordering;

public class LabeledURI implements Attribute, Comparable<LabeledURI> {
    private static final long serialVersionUID = 8935180269393438282L;

    private final URI uri;

    private final String label;

    public LabeledURI(final URI uri, @Nullable final String label) {
	this.uri = checkNotNull(uri, "uri is null");
	this.label = label;
    }

    public LabeledURI(final URI uri) {
	this(uri, null);
    }

    public URI getUri() {
	return uri;
    }

    public String getLabel() {
	return label;
    }

    @Override
    public String getOID() {
	return "1.3.6.1.4.1.250.1.57";
    }

    @Override
    public String toString() {
	if (getUri() == null)
	    return "";

	return Joiner.on(' ').skipNulls().join(new Object[] { getUri(), getLabel() });
    }

    @Override
    public String getSyntax() {
	return "1.3.6.1.4.1.1466.115.121.1.15";
    }

    public static final Ordering<String> CASE_INSENSITIVE_NULL_SAFE_ORDER = Ordering.from(String.CASE_INSENSITIVE_ORDER)
	    .nullsLast(); // or nullsFirst()

    @Override
    public int compareTo(LabeledURI that) {
	if (Objects.equal(this, that))
	    return 0;

	final int c;

	c = this.uri.compareTo(that.uri);

	if (c != 0) {
	    return c;
	}

	return  CASE_INSENSITIVE_NULL_SAFE_ORDER.compare(this.label, that.label);
    }
}
