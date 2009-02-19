package groovy.xml.dom.vm5

import org.custommonkey.xmlunit.Diff
import org.custommonkey.xmlunit.XMLUnit
import groovy.xml.*

/*
This isn't inherently doing anything requiring jdk1.5 but there is a bug in
JDK 1.4 VMs which causes the test to have problems, so it is here.
 */
class NamespaceDOMTest extends TestXmlSupport {

    def expected1 = '''
        <xsd:schema xmlns:xsd="http://www.w3.org/2001/XMLSchema">
          <xsd:element name="purchaseOrder" type="PurchaseOrderType"/>
          <xsd:element name="comment" type="xsd:string"/>
          <xsd:complexType name="PurchaseOrderType">
            <xsd:sequence>
              <xsd:element name="shipTo" type="USAddress"/>
              <xsd:element name="billTo" type="USAddress"/>
              <xsd:element minOccurs="0" ref="comment"/>
              <xsd:element name="items" type="Items"/>
            </xsd:sequence>
            <xsd:attribute name="orderDate" type="xsd:date"/>
          </xsd:complexType>
        </xsd:schema>
    '''

    def expected2 = '''
        <envelope xmlns="http://example.org/ord">
          <order>
            <number>123ABCD</number>
            <items>
              <prod:product xmlns:prod="http://example.org/prod">
                <prod:number prod:id="prod557">557</prod:number>
                <prod:name xmlns="">Short-Sleeved Woolen Blouse</prod:name>
                <prod:size system="UK-DRESS">10</prod:size>
                <prod:colour value="red"/>
              </prod:product>
            </items>
          </order>
        </envelope>
    '''

    void testXsdSchemaWithBuilderHavingAutoPrefix() {
        def builder = DOMBuilder.newInstance()
        def xsd = NamespaceBuilder.newInstance(builder, 'http://www.w3.org/2001/XMLSchema', 'xsd')
        def root = xsd.schema {
            element(name: 'purchaseOrder', type: 'PurchaseOrderType')
            element(name: 'comment', type: 'xsd:string')
            complexType(name: 'PurchaseOrderType') {
                sequence {
                    element(name: 'shipTo', type: 'USAddress')
                    element(name: 'billTo', type: 'USAddress')
                    element(minOccurs: '0', ref: 'comment')
                    element(name: 'items', type: 'Items')
                }
                attribute(name: 'orderDate', type: 'xsd:date')
            }
        }
        assertXmlEqual(expected1, XmlUtil.serialize(root))
    }

    void testXsdSchemaWithBuilderHavingMultipleNamespaces() {
        def builder = DOMBuilder.newInstance()
        def multi = NamespaceBuilder.newInstance(builder)
        multi.namespace('http://example.org/ord')
        multi.namespace('http://example.org/prod', 'prod')
        checkXml(multi)
    }

    void testXsdSchemaWithBuilderHavingDeclareNamespace() {
        def builder = DOMBuilder.newInstance()
        def multi = NamespaceBuilder.newInstance(builder)
        multi.declareNamespace(
                '':'http://example.org/ord',
                prod:'http://example.org/prod'
        )
        checkXml(multi)
    }

    private checkXml(multi) {
        def root = multi.envelope(xmlns: '') {
            order {
                number('123ABCD')
                items {
                    'prod:product' {
                        'prod:number'('prod:id': 'prod557', '557')
                        'prod:name'('Short-Sleeved Woolen Blouse')
                        'prod:size'(system: 'UK-DRESS', '10')
                        'prod:colour'(value: 'red')
                    }
                }
            }
        }
        assertXmlEqual(expected2, XmlUtil.serialize(root))
    }

    private assertXmlEqual(expected, actual) {
        XMLUnit.ignoreWhitespace = true
        def xmlDiff = new Diff(expected, actual)
        assert xmlDiff.similar(), xmlDiff.toString()
    }
}
