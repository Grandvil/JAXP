import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.*;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class XMLstaxParser {
    public static void main(String[] args) throws TransformerConfigurationException, IOException, SAXException, XMLStreamException {
        boolean ok = schemaValidator();
        if (ok) {
            File file = new File(Objects.requireNonNull(XMLstaxParser.class.getClassLoader().getResource("example2.xml")).getFile());
            Systema system = parseXMLFile(file);
            htmlGen(system);
        }
    }

    public static boolean schemaValidator() throws SAXException, IOException, XMLStreamException {
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema(new File(".\\src\\main\\resources\\XMLScheme.xsd"));
            Validator validator = schema.newValidator();
            XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(Files.newInputStream(Paths.get(".\\src\\main\\resources\\example2.xml")));
            validator.validate(new StAXSource(reader));

            System.out.println("XML is valid");
            return true;
        } catch (Exception e) {
            System.out.println("XML is not valid");
            return false;
        }
    }

    public static Systema parseXMLFile(File file) {
        Systema system = new Systema();
        List<Person> personsList = new ArrayList<>();
        Person person = null;
        Chat chat = null;
        List<Message> messageList = new ArrayList<>();
        List<Chat> chatList = new ArrayList<>();
        Content content = null;
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        try {
            XMLEventReader reader = xmlInputFactory.createXMLEventReader(new FileInputStream(file));
            while (reader.hasNext()) {
                XMLEvent xmlEvent = reader.nextEvent();
                if (xmlEvent.isStartElement()) {
                    StartElement startElement = xmlEvent.asStartElement();
                    if (startElement.getName().getLocalPart().equals("person")) {
                        person = new Person();
                        // Получаем атрибут id для каждого элемента person
                        Attribute idAttr = startElement.getAttributeByName(new QName("id"));
                        if (idAttr != null) {
                            person.id = (Integer.parseInt(idAttr.getValue()));
                        }
                    } else if (startElement.getName().getLocalPart().equals("firstName")) {
                        xmlEvent = reader.nextEvent();
                        person.firstName = (xmlEvent.asCharacters().getData());
                    } else if (startElement.getName().getLocalPart().equals("lastName")) {
                        xmlEvent = reader.nextEvent();
                        person.lastName = (xmlEvent.asCharacters().getData());
                    } else if (startElement.getName().getLocalPart().equals("pnoneNumber")) {
                        xmlEvent = reader.nextEvent();
                        person.pnoneNumber = (xmlEvent.asCharacters().getData());
                    } else if (startElement.getName().getLocalPart().equals("chat")) {
                        chat = new Chat();
                        Attribute idAttr = startElement.getAttributeByName(new QName("id"));
                        if (idAttr != null) {
                            chat.id = (Integer.parseInt(idAttr.getValue()));
                        }
                    } else if (startElement.getName().getLocalPart().equals("message")) {
                        chat.messageList = parseMessage(reader);
                    } else if (startElement.getName().getLocalPart().equals("type")) {
                        chat.type = parseType(reader);
                    }
                }
                if (xmlEvent.isEndElement()) {
                    EndElement endElement = xmlEvent.asEndElement();
                    if (endElement.getName().getLocalPart().equals("person")) {
                        personsList.add(person);
                    }
                }
                if (xmlEvent.isEndElement()) {
                    EndElement endElement = xmlEvent.asEndElement();
                    if (endElement.getName().getLocalPart().equals("chat")) {
                        chatList.add(chat);
                    }
                }
            }

        } catch (FileNotFoundException | XMLStreamException exc) {
            exc.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        system.people = personsList;
        system.chats = chatList;
        return system;
    }

    public static List<Message> parseMessage(XMLEventReader reader) throws XMLStreamException, ParseException {
        Message message = new Message();
        Content content = new Content();
        List<Message> messageList = new ArrayList<>();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SS", Locale.ENGLISH);
        while (reader.hasNext()) {
            XMLEvent xmlEvent = reader.nextEvent();
            if (xmlEvent.isStartElement()) {
                StartElement startElement = xmlEvent.asStartElement();
                if (startElement.getName().getLocalPart().equals("sender")) {
                    Attribute idAttr = startElement.getAttributeByName(new QName("idref"));
                    if (idAttr != null) {
                        message.sender = (Integer.parseInt(idAttr.getValue()));
                    }
                } else if (startElement.getName().getLocalPart().equals("receivers")) {
                    xmlEvent = reader.nextEvent();
                    xmlEvent = reader.nextEvent();
                    startElement = xmlEvent.asStartElement();
                    Attribute idAttr = startElement.getAttributeByName(new QName("idref"));
                    if (idAttr != null) {
                        message.receivers = (Integer.parseInt(idAttr.getValue()));
                    }
                } else if (startElement.getName().getLocalPart().equals("content")) {
                    message.content = content;
                    xmlEvent = reader.nextEvent();
                    xmlEvent = reader.nextEvent();
                    startElement = xmlEvent.asStartElement();
                    Attribute typeAttr = startElement.getAttributeByName(new QName("type"));
                    if (typeAttr != null) {
                        message.content.type = typeAttr.getValue();
                    }
                    typeAttr = startElement.getAttributeByName(new QName("messageEncoded"));
                    if (typeAttr != null) {
                        int val = Integer.parseInt(typeAttr.getValue());
                        if (val == 1) {
                            message.content.messageEncoded = true;
                        } else {
                            message.content.messageEncoded = false;
                        }
                    }
                    xmlEvent = reader.nextEvent();
                    message.content.content = (xmlEvent.asCharacters().getData());
                } else if (startElement.getName().getLocalPart().equals("time")) {
                    xmlEvent = reader.nextEvent();
                    String time = (xmlEvent.asCharacters().getData());
                    message.time = formatter.parse(time);
                }
            }
            if (xmlEvent.isEndElement()) {
                EndElement endElement = xmlEvent.asEndElement();
                if (endElement.getName().getLocalPart().equals("message")) {
                    messageList.add(message);
                    message = new Message();
                    content = new Content();
                }
            }

            if (xmlEvent.isEndElement()) {
                EndElement endElement = xmlEvent.asEndElement();
                if (endElement.getName().getLocalPart().equals("messageList")) {
                    break;
                }
            }
        }
        return messageList;
    }

    public static Type parseType(XMLEventReader reader) throws XMLStreamException, ParseException {
        Type type = new Type();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SS", Locale.ENGLISH);
        while (reader.hasNext()) {
            XMLEvent xmlEvent = reader.nextEvent();
            if (xmlEvent.isStartElement()) {
                StartElement startElement = xmlEvent.asStartElement();
                if (startElement.getName().getLocalPart().equals("private")) {
                    type.name = "private";
                } else if (startElement.getName().getLocalPart().equals("group")) {
                    type.name = "group";
                } else if (startElement.getName().getLocalPart().equals("dateOfCreation")) {
                    xmlEvent = reader.nextEvent();
                    String time = (xmlEvent.asCharacters().getData());
                    type.dateOfCreation = formatter.parse(time);
                } else if (startElement.getName().getLocalPart().equals("title")) {
                    xmlEvent = reader.nextEvent();
                    type.title = (xmlEvent.asCharacters().getData());
                } else if (startElement.getName().getLocalPart().equals("personRef")) {
                    Attribute idAttr = startElement.getAttributeByName(new QName("idref"));
                    if (idAttr != null) {
                        type.admin = (Integer.parseInt(idAttr.getValue()));
                    }
                }
            }

            if (xmlEvent.isEndElement()) {
                EndElement endElement = xmlEvent.asEndElement();
                if (endElement.getName().getLocalPart().equals("type")) {
                    break;
                }
            }
        }
        return type;
    }

    public static void htmlGen(Systema system) throws IOException, SAXException, TransformerConfigurationException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss.SS", Locale.ENGLISH);
        String encoding = "UTF-8";
        File file = new File(Objects.requireNonNull(XMLstaxParser.class.getClassLoader().getResource("example2.xml")).getFile());
        FileOutputStream fos = new FileOutputStream(".\\myfile.html");
        OutputStreamWriter writer = new OutputStreamWriter(fos, encoding);
        StreamResult streamResult = new StreamResult(writer);

        SAXTransformerFactory saxFactory =
                (SAXTransformerFactory) TransformerFactory.newInstance();
        TransformerHandler tHandler = saxFactory.newTransformerHandler();
        tHandler.setResult(streamResult);

        Transformer transformer = tHandler.getTransformer();
        transformer.setOutputProperty(OutputKeys.METHOD, "html");
        transformer.setOutputProperty(OutputKeys.ENCODING, encoding);
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");


        writer.write("<!DOCTYPE html>\n");
        writer.flush();
        tHandler.startDocument();
        tHandler.startElement("", "", "html", new AttributesImpl());
        tHandler.startElement("", "", "head", new AttributesImpl());
        tHandler.startElement("", "", "link rel=\"stylesheet\" href=\"mysite.css\"", new AttributesImpl());
        tHandler.startElement("", "", "title", new AttributesImpl());
        tHandler.characters("Состояния чатов".toCharArray(), 0, "Состояния чатов".length());
        tHandler.endElement("", "", "title");
        tHandler.endElement("", "", "head");
        tHandler.startElement("", "", "body", new AttributesImpl());

        //Заголовок
        tHandler.startElement("", "", "h1", new AttributesImpl());
        tHandler.characters("Пользователи".toCharArray(), 0, "Пользователи".length());
        tHandler.endElement("", "", "h1");

        //Таблица
        tHandler.startElement("", "", "table", new AttributesImpl());
        //ID
        tHandler.startElement("", "", "td", new AttributesImpl());
        tHandler.characters("ID".toCharArray(), 0, "ID".length());
        tHandler.endElement("", "", "td");
        //Имя
        tHandler.startElement("", "", "td", new AttributesImpl());
        tHandler.characters("Имя".toCharArray(), 0, "Имя".length());
        tHandler.endElement("", "", "td");
        //Фамилия
        tHandler.startElement("", "", "td", new AttributesImpl());
        tHandler.characters("Фамилия".toCharArray(), 0, "Фамилия".length());
        tHandler.endElement("", "", "td");
        //Номер телефона
        tHandler.startElement("", "", "td", new AttributesImpl());
        tHandler.characters("Номер телефона".toCharArray(), 0, "Номер телефона".length());
        tHandler.endElement("", "", "td");


        for (Person i : system.people) {
            tHandler.startElement("", "", "tr", new AttributesImpl());
            //ID
            tHandler.startElement("", "", "td", new AttributesImpl());
            tHandler.characters(Integer.toString(i.id).toCharArray(), 0, Integer.toString(i.id).length());
            tHandler.endElement("", "", "td");
            //Имя
            tHandler.startElement("", "", "td", new AttributesImpl());
            tHandler.characters(i.firstName.toCharArray(), 0, i.firstName.length());
            tHandler.endElement("", "", "td");
            //Фамилия
            tHandler.startElement("", "", "td", new AttributesImpl());
            tHandler.characters(i.lastName.toCharArray(), 0, i.lastName.length());
            tHandler.endElement("", "", "td");
            //Номер телефона
            tHandler.startElement("", "", "td", new AttributesImpl());
            tHandler.characters(i.pnoneNumber.toCharArray(), 0, i.pnoneNumber.length());
            tHandler.endElement("", "", "td");


            tHandler.endElement("", "", "tr");
        }
        tHandler.startElement("", "", "tr", new AttributesImpl());
        tHandler.startElement("", "", "th", new AttributesImpl());
        tHandler.endElement("", "", "th");
        tHandler.startElement("", "", "td", new AttributesImpl());
        tHandler.characters("Всего пользователей".toCharArray(), 0, "Всего пользователей".length());
        tHandler.endElement("", "", "td");
        tHandler.startElement("", "", "td", new AttributesImpl());
        tHandler.characters(Integer.toString(system.people.size()).toCharArray(), 0, Integer.toString(system.people.size()).length());
        tHandler.endElement("", "", "td");
        tHandler.endElement("", "", "tr");

        tHandler.endElement("", "", "table");

        for (int i = 0; i < system.chats.size(); i++) {
            //Заголовок
            tHandler.startElement("", "", "h1", new AttributesImpl());
            if (system.chats.get(i).type.name == "group") {
                String chatName = ("Чат " + system.chats.get(i).id + " | Тип чата: " + system.chats.get(i).type.name) +
                        " | Чат создан: " + formatter.format(system.chats.get(i).type.dateOfCreation) + " | Название чата: " + system.chats.get(i).type.title;
                tHandler.characters(chatName.toCharArray(),
                        0,
                        chatName.length());
            } else {
                String chatName = ("Чат " + system.chats.get(i).id + " | Тип чата: " + system.chats.get(i).type.name) +
                        " | Чат создан: " + formatter.format(system.chats.get(i).type.dateOfCreation);
                tHandler.characters(chatName.toCharArray(),
                        0,
                        chatName.length());
            }
            tHandler.endElement("", "", "h1");

            //Таблица 2
            tHandler.startElement("", "", "table", new AttributesImpl());
            tHandler.startElement("", "", "tr", new AttributesImpl());
            //ID отправителя
            tHandler.startElement("", "", "td", new AttributesImpl());
            tHandler.characters("ID отправителя".toCharArray(), 0, "ID отправителя".length());
            tHandler.endElement("", "", "td");
            //ID получателя
            tHandler.startElement("", "", "td", new AttributesImpl());
            tHandler.characters("ID получателя".toCharArray(), 0, "ID получателя".length());
            tHandler.endElement("", "", "td");
            //Время сообщения
            tHandler.startElement("", "", "td", new AttributesImpl());
            tHandler.characters("Время сообщения".toCharArray(), 0, "Время сообщения".length());
            tHandler.endElement("", "", "td");
            //Тип сообщения
            tHandler.startElement("", "", "td", new AttributesImpl());
            tHandler.characters("Тип сообщения".toCharArray(), 0, "Тип сообщения".length());
            tHandler.endElement("", "", "td");
            //Кодировка
            tHandler.startElement("", "", "td", new AttributesImpl());
            tHandler.characters("Кодировка".toCharArray(), 0, "Кодировка".length());
            tHandler.endElement("", "", "td");
            //Контент сообщения
            tHandler.startElement("", "", "td", new AttributesImpl());
            tHandler.characters("Контент сообщения".toCharArray(), 0, "Контент сообщения".length());
            tHandler.endElement("", "", "td");
            tHandler.endElement("", "", "tr");

            for (Message m : system.chats.get(i).messageList) {
                tHandler.startElement("", "", "tr", new AttributesImpl());

                //ID отправителя
                tHandler.startElement("", "", "td", new AttributesImpl());
                tHandler.characters(Integer.toString(m.sender).toCharArray(), 0, Integer.toString(m.sender).length());
                tHandler.endElement("", "", "td");
                //ID получателя
                tHandler.startElement("", "", "td", new AttributesImpl());
                tHandler.characters(Integer.toString(m.receivers).toCharArray(), 0, Integer.toString(m.receivers).length());
                tHandler.endElement("", "", "td");
                //Время сообщения
                tHandler.startElement("", "", "td", new AttributesImpl());
                tHandler.characters(formatter.format(m.time).toCharArray(), 0, formatter.format(m.time).length());
                tHandler.endElement("", "", "td");
                //Тип сообщения
                tHandler.startElement("", "", "td", new AttributesImpl());
                tHandler.characters(m.content.type.toCharArray(), 0, m.content.type.length());
                tHandler.endElement("", "", "td");
                //Кодировка
                tHandler.startElement("", "", "td", new AttributesImpl());
                tHandler.characters(Boolean.toString(m.content.messageEncoded).toCharArray(), 0, Boolean.toString(m.content.messageEncoded).length());
                tHandler.endElement("", "", "td");
                //Контент сообщения
                tHandler.startElement("", "", "td", new AttributesImpl());
                tHandler.characters(m.content.content.toCharArray(), 0, m.content.content.length());
                tHandler.endElement("", "", "td");

                tHandler.endElement("", "", "tr");
            }
            tHandler.endElement("", "", "table");
        }

        tHandler.endElement("", "", "body");
        tHandler.endElement("", "", "html");
        tHandler.endDocument();
        writer.close();

        fos.close();
    }

}
