import nl.prikkeldraad.git.VersionParser

@RunWith
class VersionParserTestCase extends GroovyTestCase {
    private vp
    
    void setUp() {
        this.vp = new VersionParser(
            "master, 
            "1.2.3", 
            1234)
                
        this.vp.parse()
    }

    @Test
    void testFullFormat() {
        def number = this.vp.format("%M.%m.%p-%b")
        assertEquals(number, "1.2.3-1234")
    }    

    void testNumberFormat() {
        def number = this.vp.format("%M.%m.%p")
        assertEquals(number, "1.2.3")
    }    
    
    void testMajorFormat() {
       def number = this.vp.format("%M")
       assertEquals(number, "1")
    }
}
