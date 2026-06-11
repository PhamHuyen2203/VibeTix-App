
import subprocess
import xml.etree.ElementTree as ET

try:
    # 1. Huyen's strings
    huyen_xml = subprocess.check_output(['git', 'show', 'c778b8e^2:app/src/main/res/values/strings.xml']).decode('utf-8')
    root1 = ET.fromstring(huyen_xml)
    
    # 2. My strings
    my_xml = subprocess.check_output(['git', 'show', 'c0ad8f1:app/src/main/res/values/strings.xml']).decode('utf-8')
    root2 = ET.fromstring(my_xml)
    
    # 3. Merge
    names1 = set(child.attrib.get('name') for child in root1 if 'name' in child.attrib)
    for child in root2:
        name = child.attrib.get('name')
        if name and name not in names1:
            root1.append(child)
            
    # 4. Write back
    with open('app/src/main/res/values/strings.xml', 'w', encoding='utf-8') as f:
        f.write('<?xml version=\"1.0\" encoding=\"utf-8\"?>\n')
        f.write(ET.tostring(root1, encoding='unicode'))
        
    print('Merged strings successfully via Python')
except Exception as e:
    print('Error:', e)

