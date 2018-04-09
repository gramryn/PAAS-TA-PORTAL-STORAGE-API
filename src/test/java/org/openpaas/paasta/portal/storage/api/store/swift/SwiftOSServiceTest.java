package org.openpaas.paasta.portal.storage.api.store.swift;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.javaswift.joss.client.mock.AccountMock;
import org.javaswift.joss.client.mock.ContainerMock;
import org.javaswift.joss.model.StoredObject;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.Mock;
import org.openpaas.paasta.portal.storage.api.AbstractTest;
import org.openpaas.paasta.portal.storage.api.config.SwiftOSConstants.ResultStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SwiftOSServiceTest extends AbstractTest {
    private static final String multipartTempFilename = "tmp-multipart-abcdef-test-1234.txt";
    private static final String filename = "test-1234.txt";
    private static final String contentType = MediaType.TEXT_PLAIN_VALUE;
    private static final byte[] contents = "Hello object storage".getBytes();
    
    @Mock private AccountMock account; 
    @Mock private ContainerMock container;
    @Mock private SwiftOSService swiftOSService;
    @Mock private MultipartFile multipartFile;
    
    @Override
    public void setUp() throws Exception {
        account = new AccountMock();
        //account.setPublicHost( "http://127.0.0.1" );
        //account.setPreferredRegion( "Public" );
        
        container = (ContainerMock) new ContainerMock( account, "test-container" );
        if (false == container.exists()) {
            container.create();
            container.makePublic();
        }
        swiftOSService = new SwiftOSService( container );
        
        multipartFile = new MockMultipartFile( multipartTempFilename, filename, contentType, contents );
    }
    
    @Override
    public void tearDown() throws Exception {
        multipartFile.getInputStream().close();
        
        if (container.exists())
            container.delete();
    }
    
    private final SwiftOSFileInfo uploadObjectMock() {
        return swiftOSService.putObject( filename, new ByteArrayInputStream( contents ), contentType );
    }
    
    private final boolean removeObjectMock(String filename) {
        try {
            StoredObject object = container.getObject( filename );
            if (object.exists())
                object.delete();
            
            return true;
        } catch (final Throwable e) {
            return false;
        }
    }
    
    @Test
    public void testPutObject() throws IOException {
        final SwiftOSFileInfo fileInfo = swiftOSService.putObject( multipartFile );
        assertEquals( fileInfo.getFilename(), filename );
        assertTrue( fileInfo.getStoredFilename().contains( filename ) );
        assertEquals( fileInfo.getFileType(), contentType );
        assertEquals( fileInfo.getLength(), contents.length );
        assertEquals( fileInfo.getResultStatus(), ResultStatus.SUCCESS );
        
        final SwiftOSFileInfo fileInfo2 = swiftOSService.putObject( filename, new ByteArrayInputStream( contents ), contentType );
        assertEquals( fileInfo2.getFilename(), filename );
        assertTrue( fileInfo2.getStoredFilename().contains( filename ) );
        assertEquals( fileInfo2.getFileType(), contentType );
        assertEquals( fileInfo2.getLength(), contents.length );
        assertEquals( fileInfo2.getResultStatus(), ResultStatus.SUCCESS );
        
        removeObjectMock( fileInfo.getStoredFilename() );
        removeObjectMock( fileInfo2.getStoredFilename() );
    }
    
    @Test
    public void testGetObject() throws FileNotFoundException {
        final SwiftOSFileInfo generateFileInfo = uploadObjectMock();
        
        final SwiftOSFileInfo fileInfo = swiftOSService.getObject( generateFileInfo.getStoredFilename() );
        assertEquals( fileInfo.getFilename(), filename );
        assertTrue( fileInfo.getStoredFilename().contains( filename ) );
        assertEquals( fileInfo.getFileType(), contentType );
        assertEquals( fileInfo.getLength(), contents.length );
        assertEquals( fileInfo.getResultStatus(), ResultStatus.SUCCESS );
        
        removeObjectMock( fileInfo.getStoredFilename() );
    }
    
    @Test
    public void testUpdateObject() {
        try {
            swiftOSService.updateObject( swiftOSService.generateStoredFilename( filename ), multipartFile );
        } catch (final Throwable e) {
            assertTrue(e instanceof UnsupportedOperationException);
        }
    }
    
    @Test
    public void testRemoveObject() {
        final SwiftOSFileInfo fileInfo = uploadObjectMock();
        boolean isRemove = swiftOSService.removeObject( fileInfo.getStoredFilename() );
        // (isRemove == false)? "Remove operation fails." : "";
        assertTrue( isRemove );
        
        removeObjectMock( fileInfo.getStoredFilename() );
    }
    
    @Test
    public void testGenerateFilenameAndExtractOriginalFilename() throws Exception {
        final String[] filenames = {
            "test1234.png", "test-1234.png", "test-1234.bak.png",
            ".test1234.png", ".test-1234.png", ".test-1234.bak.png",
            "-test1234.png", "-test-1234.png", "-test-1234.bak.png",
        };
        
        for (final String filename : filenames) {
            String storedFilename = swiftOSService.generateStoredFilename( filename );
            String restoreFilename = swiftOSService.getOriginalFilename( storedFilename );
            logger.info( "original filename : {}", filename );
            logger.info( "stored filename : {}", storedFilename );
            logger.info( "restored original filename : {}", restoreFilename );
            
            assertTrue( restoreFilename.equals( filename ) );
        }
    }
}
