
import xml.etree.ElementTree as ET

try:
    with open('colors_huyen.xml', 'r', encoding='utf-16') as f:
        tree1 = ET.parse(f)
    root1 = tree1.getroot()
    with open('colors_me.xml', 'r', encoding='utf-16') as f:
        tree2 = ET.parse(f)
    root2 = tree2.getroot()
    
    names1 = set(child.attrib.get('name') for child in root1 if 'name' in child.attrib)
    for child in root2:
        name = child.attrib.get('name')
        if name and name not in names1:
            root1.append(child)
            
    with open('app/src/main/res/values/colors.xml', 'w', encoding='utf-8') as f:
        f.write('<?xml version=\"1.0\" encoding=\"utf-8\"?>\n')
        f.write(ET.tostring(root1, encoding='unicode'))
        
    print('Merged colors successfully')
except Exception as e:
    print('Error:', e)

