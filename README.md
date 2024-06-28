# Description du projet
 Le présent projet vise à créer un plugin maven devant servir à la génération
 des composants TypeScript qui sont la fondation du module angular. Il scanne le projet source 
 et génère des composants spécifiques parmis lesquels:
 
*   Les **VD**(Value Domaine):
*   Les **Bean**:
*	Le composants de **Message**: 
*	Le **FieldMetadata**: 

# Paramètres requis
1.  **source.root.package**: Il s'agit du nom du package à scaner pendant pour indentifier les composants sources. La valeur par défaut est "com.agro360"
2.  **ts.project.app.dir**: C'est le repertoire dans le projet web qui utilisera les composants générés. La valeur par défaut est "C:/WorkSpace/0-projects/0-business/agro360v2/agro360-web-client/src/app/"


## Pour exécuter le plugin
#### Pour la prémière fois:
Lorsque vous venez de cloner le projet à partir du serveur git, exécuter la commande `mvn clean install`.

#### Pour les autres fois:
Aller à la racine du projet agro360-bo
Exécuter la commande `mvn com.agro360:bean2ts-maven-plugin:gtc ` toutes les autres fois
