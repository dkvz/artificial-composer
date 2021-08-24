package artificialcomposer;

import java.util.*;
import java.io.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.StreamResult;

/**
 *
 * @author William
 */
public class MusicScore {

    private ArrayList<Note> notes = null;
    private int tempo = 0;
    private File fil;
    private String name;
    // Je peux pas faire comme ça sinon windows croit que le fichier reste ouvert...
    //private Document doc = null;

    public MusicScore() {
        // Rien.
    }
    
    public MusicScore(File fil) {
        this.fil = fil;
    }

    public void notesToXML(int tempo, String recName) throws FileNotFoundException, IOException {
        this.tempo = tempo;
        FileOutputStream fos = new FileOutputStream(fil);
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(fos, "UTF-8"));
            out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            out.write("<music-score>\n");
            out.write("\t<info>\n");
            out.write("\t\t<record-name>" + recName + "</record-name>\n");
            out.write("\t\t<original-tempo>" + tempo + "</original-tempo>\n");
            out.write("\t</info>\n");
            out.write("\t<score>\n");
            if (notes != null) {
                for (int x = 0; x < notes.size(); x++) {
                    out.write("\t\t<note>\n");
                    out.write("\t\t\t<name>" + notes.get(x).getNoteString() + "</name>\n");
                    out.write("\t\t\t<octave>" + notes.get(x).getOctave() + "</octave>\n");
                    out.write("\t\t\t<duration>" + notes.get(x).getNoteValue() + "</duration>\n");
                    out.write("\t\t</note>\n");
                }
            }
            out.write("\t</score>\n");
            out.write("</music-score>\n");
        } catch (UnsupportedEncodingException ex) {
            // Fuck that.
        } finally {
            if (out != null) {
                out.close();
            }
            fos.close();
            System.gc();
        }
    }

    public void XMLToNotes() {
        setNotes((ArrayList<Note>) new ArrayList());
        if (!fil.exists()) {
            return;
        }
        Document document = parseTheShit();
        Element root = document.getDocumentElement();
        NodeList listNotes = root.getElementsByTagName("note");
        NodeList infoL = root.getElementsByTagName("info");
        if (infoL != null) {
            Node temp = infoL.item(0);
            if (temp.getNodeName().equals("info")) {
                NodeList infos = temp.getChildNodes();
                for (int x = 0; x < infos.getLength(); x++) {
                    if (infos.item(x).getNodeName().equals("original-tempo")) {
                        try {
                            setTempo((int) new Integer(infos.item(x).getTextContent()));
                        } catch (NumberFormatException ex) {
                            setTempo((int) new Integer(0));
                        }
                    }
                }
            }
        }
        for (int x = 0; x < listNotes.getLength(); x++) {
            NodeList noteSpecs = listNotes.item(x).getChildNodes();
            String noteStr = "";
            int duration = 0;
            int octave = 0;
            for (int y = 0; y < noteSpecs.getLength(); y++) {
                Node el = noteSpecs.item(y);
                if (el.getNodeName().equals("name")) {
                    noteStr = el.getTextContent();
                } else if (el.getNodeName().equals("octave")) {
                    try {
                        octave = Integer.parseInt(el.getTextContent());
                    } catch (NumberFormatException ex) {
                        // Rien.
                        }
                } else if (el.getNodeName().equals("duration")) {
                    try {
                        duration = Integer.parseInt(el.getTextContent());
                    } catch (NumberFormatException ex) {
                        // Rien.
                    }
                }
            }
            Note note = new Note(noteStr, octave);
            note.setNoteValue(duration);
            getNotes().add(note);
        }
        document = null;
        System.gc();
    }

    private Document parseTheShit() {
        DocumentBuilderFactory fabrique = DocumentBuilderFactory.newInstance();
        Document doc = null;
        try {
            DocumentBuilder constructeur = fabrique.newDocumentBuilder();
            doc = constructeur.parse(this.fil);
        } catch (Exception e) {
            // Pas cool ça.
        }
        return doc;
    }
    
    public void newNote(Note note, int index) {
        Document doc = parseTheShit();
        if (notes != null && index <= notes.size() && doc != null && fil.exists()) {
            // Si index = 0; on ajoute au début...
            Element root = doc.getDocumentElement();
            NodeList listNotes = root.getElementsByTagName("note");
            NodeList theScore = root.getElementsByTagName("score");
            if (theScore.getLength() > 0) {
                Element newNote = doc.createElement("note");
                Element noteName = doc.createElement("name");
                Element noteDuration = doc.createElement("duration");
                Element noteOctave = doc.createElement("octave");
                noteName.setTextContent(note.getNoteString());
                noteDuration.setTextContent(Integer.toString(note.getNoteValue()));
                noteOctave.setTextContent(Integer.toString(note.getOctave()));
                newNote.appendChild(noteName);
                newNote.appendChild(noteDuration);
                newNote.appendChild(noteOctave);
                if (listNotes.getLength() > 0 && index != notes.size()) {
                    Element theNote = (Element) listNotes.item(index);
                    theScore.item(0).insertBefore(newNote, theNote);
                } else {
                    theScore.item(0).appendChild(newNote);
                }
                transform(doc);
                notes.add(index, note);
            }
        }
    }

    public void updateNote(Note note, int index) {
        Document doc = parseTheShit();
        if (notes != null && !notes.isEmpty() && index < notes.size() && doc != null && fil.exists()) {
            Element root = doc.getDocumentElement();
            NodeList listNotes = root.getElementsByTagName("note");
            NodeList theScore = root.getElementsByTagName("score");
            if (theScore.getLength() > 0) {
                Element theNote = (Element) listNotes.item(index);
                Element newNote = doc.createElement("note");
                Element noteName = doc.createElement("name");
                Element noteDuration = doc.createElement("duration");
                Element noteOctave = doc.createElement("octave");
                noteName.setTextContent(note.getNoteString());
                noteDuration.setTextContent(Integer.toString(note.getNoteValue()));
                noteOctave.setTextContent(Integer.toString(note.getOctave()));
                newNote.appendChild(noteName);
                newNote.appendChild(noteDuration);
                newNote.appendChild(noteOctave);
                theScore.item(0).replaceChild(newNote, theNote);
                transform(doc);
                notes.remove(index);
                notes.add(index, note);
            }
        }
    }

    private void transform(Document doc) {
        Source source = new DOMSource(doc);
        Result resultat = new StreamResult(this.fil);
        TransformerFactory transfabrik = TransformerFactory.newInstance();
        try {
            Transformer transformer = transfabrik.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.transform(source, resultat);
            transformer = null;
        } catch (TransformerException ex) {
            // Caca.
        } finally {
            doc = null;
            source = null;
            resultat = null;
            System.gc();
        }
    }

    public void updateTempo(int newTempo) {
        if (newTempo > 0 && tempo != 0 && fil.exists()) {
            Document doc = parseTheShit();
            if (doc != null) {
                Element root = doc.getDocumentElement();
                NodeList info = root.getElementsByTagName("info");
                if (info.getLength() > 0) {
                    Element infoEl = (Element) info.item(0);
                    NodeList tempoL = infoEl.getElementsByTagName("original-tempo");
                    if (tempoL.getLength() > 0) {
                        Element theTempo = doc.createElement("original-tempo");
                        tempo = newTempo;
                        theTempo.setTextContent(Integer.toString(tempo));
                        infoEl.replaceChild(theTempo, tempoL.item(0));
                        transform(doc);
                    }
                }
            }
        }
    }

    public void removeNote(int index) {
        Document doc = parseTheShit();
        if (notes != null && !notes.isEmpty() && index < notes.size() && doc != null && fil.exists()) {
            Element root = doc.getDocumentElement();
            NodeList listNotes = root.getElementsByTagName("note");
            NodeList theScore = root.getElementsByTagName("score");
            if (theScore.getLength() > 0) {
                Element theNote = (Element) listNotes.item(index);
                theScore.item(0).removeChild(theNote);
                transform(doc);
                notes.remove(index);
            }
        }
    }
    
    @Override
    public String toString() {
        if (fil != null) {
            return fil.getName();
        }
        return getName();
    }

    public ArrayList<Note> getNotes() {
        return notes;
    }

    public void setNotes(ArrayList<Note> notes) {
        this.notes = notes;
    }

    public int getTempo() {
        return tempo;
    }

    public void setTempo(int tempo) {
        this.tempo = tempo;
    }

    public File getFil() {
        return fil;
    }

    public void setFil(File fil) {
        this.fil = fil;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
