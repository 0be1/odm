package fr.mtlx.odm.it;

/*
 * #%L
 * fr.mtlx.odm
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2012 - 2013 Alexandre Mathieu <me@mtlx.fr>
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import java.io.File;
import java.util.HashSet;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.core.schema.SchemaPartition;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.ldif.extractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.ldif.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.loader.ldif.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.shared.ldap.schema.registries.SchemaLoader;

public final class EmbeddedDS
{
	
	public final static String PARTITION = "dc=mtlx,dc=fr";
	
    /** The directory service */
    private DirectoryService service;

    /** The LDAP server */
    protected LdapServer server;


    /**
     * Add a new partition to the server
     *
     * @param partitionId The partition Id
     * @param partitionDn The partition DN
     * @return The newly added partition
     * @throws Exception If the partition can't be added
     */
    private Partition addPartition( String partitionId, String partitionDn ) throws Exception
    {
        // Create a new partition named 'foo'.
        JdbmPartition partition = new JdbmPartition();
        partition.setId( partitionId );
        partition.setPartitionDir( new File( service.getWorkingDirectory(), partitionId ) );
        partition.setSuffix( partitionDn );
        service.addPartition( partition );

        return partition;
    }


    /**
     * Add a new set of index on the given attributes
     *
     * @param partition The partition on which we want to add index
     * @param attrs The list of attributes to index
     */
    private void addIndex( Partition partition, String... attrs )
    {
        // Index some attributes on the apache partition
        HashSet<Index<?, ServerEntry, Long>> indexedAttributes = new HashSet<Index<?, ServerEntry, Long>>();

        for ( String attribute : attrs )
        {
            indexedAttributes.add( new JdbmIndex<String, ServerEntry>( attribute ) );
        }

        ( ( JdbmPartition ) partition ).setIndexedAttributes( indexedAttributes );
    }

    
    /**
     * initialize the schema manager and add the schema partition to directory service
     *
     * @throws Exception if the schema LDIF files are not found on the classpath
     */
    private void initSchemaPartition() throws Exception
    {
        SchemaPartition schemaPartition = service.getSchemaService().getSchemaPartition();

        // Init the LdifPartition
        LdifPartition ldifPartition = new LdifPartition();
        String workingDirectory = service.getWorkingDirectory().getPath();
        ldifPartition.setWorkingDirectory( workingDirectory + "/schema" );

        // Extract the schema on disk (a brand new one) and load the registries
        File schemaRepository = new File( workingDirectory, "schema" );
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( new File( workingDirectory ) );
        extractor.extractOrCopy( true );

        schemaPartition.setWrappedPartition( ldifPartition );

        SchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        SchemaManager schemaManager = new DefaultSchemaManager( loader );
        service.setSchemaManager( schemaManager );

        // We have to load the schema now, otherwise we won't be able
        // to initialize the Partitions, as we won't be able to parse 
        // and normalize their suffix DN
        schemaManager.loadAllEnabled();

        schemaPartition.setSchemaManager( schemaManager );

        List<Throwable> errors = schemaManager.getErrors();

        if ( errors.size() != 0 )
        {
            throw new Exception( "Schema load failed : " + errors );
        }
    }
    
    
    /**
     * Initialize the server. It creates the partition, adds the index, and
     * injects the context entries for the created partitions.
     *
     * @param workDir the directory to be used for storing the data
     * @throws Exception if there were some problems while initializing the system
     */
    private void initDirectoryService( @Nullable File workDir ) throws Exception
    {
        // Initialize the LDAP service
        service = new DefaultDirectoryService();
        
        if ( workDir != null )
        {
        	service.setWorkingDirectory( workDir );
        }
        
        // first load the schema
        initSchemaPartition();
        
        // then the system partition
        // this is a MANDATORY partition
        Partition systemPartition = addPartition( "system", ServerDNConstants.SYSTEM_DN );
        service.setSystemPartition( systemPartition );
        
        // Disable the ChangeLog system
        service.getChangeLog().setEnabled( false );
        service.setDenormalizeOpAttrsEnabled( true );

        Partition partition = addPartition( "mtlx", PARTITION );

        // Index some attributes on the apache partition
        addIndex( partition, "objectClass", "ou", "uid" );

        // And start the service
        service.startup();

        // Inject the foo root entry if it does not already exist
        try
        {
            service.getAdminSession().lookup( partition.getSuffixDn() );
        }
        catch ( LdapException lnnfe )
        {
            DN dnFoo = new DN( PARTITION );
            ServerEntry entryFoo = service.newEntry( dnFoo );
            entryFoo.add( "objectClass", "top", "domain", "extensibleObject" );
            entryFoo.add( "dc", "mtlx" );
            service.getAdminSession().add( entryFoo );
        }

        
        // We are all done !
    }


    
    /**
     * starts the LdapServer
     *
     * @throws Exception
     */
    public void startServer() throws Exception
    {
        server = new LdapServer();
        int serverPort = 10389;
        server.setTransports( new TcpTransport( serverPort ) );
        server.setDirectoryService( service );
        
        server.start();
    }

 
    public EmbeddedDS(@Nullable File workDir) throws Exception 
    {
    	if (workDir != null)
    	{
            workDir = new File( System.getProperty( "java.io.tmpdir" ) + "/server-work" );
            workDir.mkdirs();
    	}
    	
       initDirectoryService( workDir );
       
       startServer();
    }


	public LdapServer getServer()
	{
		return server;
	}
}
