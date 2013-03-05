package com.apartech.auraconfig.resources.customcode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.apartech.auraconfig.resources.DiffAttribute;
import com.apartech.auraconfig.resources.Resource;
import com.apartech.auraconfig.resources.ResourceCreatorHelper;
import com.apartech.auraconfig.resources.ResourceDiffReportHelper;
import com.apartech.auraconfig.resources.ResourceHelper;
import com.apartech.auraconfig.resources.WASConfigReader;
import com.apartech.auraconfig.resources.WASConfigReaderHelper;
import com.apartech.common.deploy.DeployInfo;
import com.apartech.common.exception.DeployException;
import com.apartech.common.properties.helper.PropertyHelper;
import com.ibm.websphere.management.AdminClient;
import com.ibm.websphere.management.Session;
import com.ibm.websphere.management.configservice.ConfigService;
import com.ibm.websphere.management.configservice.ConfigServiceHelper;
import com.ibm.websphere.management.exception.ConfigServiceException;
import com.ibm.websphere.management.exception.ConnectorException;

public class SIForeignBusCustomCodeImpl {
	private static final Log logger = LogFactory.getLog(SIForeignBusCustomCodeImpl.class);
	WASConfigReaderHelper wasConfigReaderHelper = new WASConfigReaderHelper();

	/**
	 * 
	 * Arguments:
	  *bus - Bus name.
	  *name - Foreign bus name.
	  *routingType - Routing type name {Direct | Indirect}.
	  type - Type name {MQ | SIBus} (only required if routing type is "Direct").
	  description - Description.
	  sendAllowed - Send allowed {True | False} (default is "True").
	  inboundUserid - Inbound user identifier.
	  outboundUserid - Outbound user identifier.
	  nextHopBus - Next hop bus name.
	
	Steps:
	   destinationDefaults - Sets the destination defaults on a SIB foreign bus.
	   topicSpaceMappings - Sets the topic space mappings on a SIB foreign bus.
	 */
	
	/**
	 * 
	 * @param session
	 * @param configService
	 * @param resource
	 * @param configObject
	 * @param deployInfo
	 * @return
	 * @throws DeployException
	 */
	public HashMap<String, String> extract (Session session,ConfigService configService,
			Resource resource,ObjectName configObject,DeployInfo deployInfo ,HashMap<String, String> incomingAttributeList) 
			throws DeployException,MalformedObjectNameException,AttributeNotFoundException{

		//	attributes =  PropertyHelper.getArrayFromCommaSeperated(resource.getResourceMetaData().getCustomCodeAttributes() );
		try {
			//System.out.println( " Attribute Size for "  + resource.getContainmentPath() + " is " + resource.getAttributeList().size());
			logger.trace( " Get virtualLink "  );
			AttributeList attributeValue = (AttributeList)configService.getAttribute(session,configObject,"virtualLink");
			if (attributeValue !=null){
				ObjectName attributeValueObject = ConfigServiceHelper.createObjectName(attributeValue); 
				System.out.println(" ************ " +  ConfigServiceHelper.getConfigDataType(attributeValueObject));

				String configType = ConfigServiceHelper.getConfigDataType(attributeValueObject);
				String routingType = "Indirect";
				String type;
				if (configType.equalsIgnoreCase("SIBVirtualMQLink")){
					incomingAttributeList.put("routingType", "MQ");
					incomingAttributeList.put("type", "Direct");
					checkInCommingAttribute(attributeValueObject,incomingAttributeList, resource, 
							configService, session ,deployInfo, 0,"SIBVirtualMQLink" );
				
				}else if (configType.equalsIgnoreCase("SIBVirtualGatewayLink")){
					incomingAttributeList.put("routingType", "SIBus");
					incomingAttributeList.put("type", "Direct");
					checkInCommingAttribute(attributeValueObject,incomingAttributeList, resource, 
							configService, session ,deployInfo, 0,"SIBVirtualGatewayLink" );

				}
			}else{
				ObjectName nextHopForeignBusObjectName = (ObjectName)configService.getAttribute(session,configObject,"nextHop");
				incomingAttributeList.put("nextHop", configService.getAttribute(session,nextHopForeignBusObjectName ,"name").toString());
				
				incomingAttributeList.put("type", "Indirect");
				System.out.println(" Foreign bus must be indirect as virtual link is null ");
			}
			
			
		}catch(ConfigServiceException e){
			throw new DeployException(e);
		}catch(ConnectorException e){
			throw new DeployException(e);
		}
		return incomingAttributeList;
	}
	
	
	/**
	 * 
	 * @param session
	 * @param configService
	 * @param resource
	 * @param configObject
	 * @param deployInfo
	 * @throws DeployException
	 */
	public ArrayList<DiffAttribute> modify(Session session,ConfigService configService,
			Resource resource,ObjectName configObject,DeployInfo deployInfo,
			 AdminClient adminClient,ObjectName scope, Resource allResources,Resource referenceResources) throws DeployException{
	//	attributes =  PropertyHelper.getArrayFromCommaSeperated(resource.getResourceMetaData().getCustomCodeAttributes() );
		System.out.println(" Get the attriutes");
		
		
		ArrayList modifiedAttributes = new ArrayList();
		ResourceDiffReportHelper resourceDiffReportHelper = new ResourceDiffReportHelper();
		try {
			
			// Start - Modify Attributes of SIBMQLinkSenderChannel
			AttributeList virtualLinkAttributeList =  (AttributeList )configService.getAttribute(session, configObject, "virtualLink");
			ObjectName attrObjectName = ConfigServiceHelper.createObjectName((AttributeList)virtualLinkAttributeList );
		
			modifyObjectName("SIBVirtualMQLink", attrObjectName, modifiedAttributes, resource, configService, session, adminClient, 
					scope, allResources, referenceResources, deployInfo);

			
			modifyObjectName("SIBForeignBus", configObject, modifiedAttributes, resource, configService, session, adminClient, 
					scope, allResources, referenceResources, deployInfo);	

			
		}catch(ConfigServiceException e){
			throw new DeployException(e);
		}catch(ConnectorException e){
			throw new DeployException(e);
		}catch(AttributeNotFoundException e){
			throw new DeployException(e);
		}
		return modifiedAttributes;
	}

	
	/**
	 * 
	 * @param objectType
	 * @param resourceWasObject
	 * @param modifiedAttributes
	 * @param resource
	 * @param configService
	 * @param session
	 * @param adminClient
	 * @param scope
	 * @param allResources
	 * @param referenceResources
	 * @param deployInfo
	 * @throws AttributeNotFoundException
	 * @throws ConnectorException
	 * @throws ConfigServiceException
	 * @throws DeployException
	 */
	private void modifyObjectName(String objectType, ObjectName resourceWasObject, 
			ArrayList<DiffAttribute> modifiedAttributes,Resource resource,
			ConfigService configService, Session session, AdminClient adminClient,ObjectName scope, 
			Resource allResources,Resource referenceResources,DeployInfo deployInfo )
			throws AttributeNotFoundException,ConnectorException,ConfigServiceException,DeployException{
		
		AttributeList changedAttrList = new AttributeList ();
		AttributeList attributeMetaInfo =  configService.getAttributesMetaInfo(objectType);
		Iterator attrMetaInfoListIterator = attributeMetaInfo.iterator();
		
		while (attrMetaInfoListIterator.hasNext()){
			Attribute configObjectAttributeMetaInfo = (Attribute)attrMetaInfoListIterator.next();
			String resourceAttributeName = configObjectAttributeMetaInfo.getName();
			logger.trace( " Checking if attribute needs to be modified " + resourceAttributeName  );
			if (!(resourceAttributeName.equalsIgnoreCase("name") && resource.getResourceMetaData().getType().equalsIgnoreCase("SIBVirtualMQLink"))){
				modifyAttribute(resourceWasObject, resourceAttributeName,modifiedAttributes,resource,
						configService, changedAttrList,session, adminClient,scope, 
						allResources,referenceResources,deployInfo);
			}
			
		}
		configService.setAttributes(session, resourceWasObject, changedAttrList);
		
	}
	
	/**
	 * 
	 * @param configObject
	 * @param atributeName
	 * @param modifiedAttributes
	 * @param resource
	 * @throws AttributeNotFoundException
	 */
	private void modifyAttribute(ObjectName resourceWasObject, String resourceAttributeName,ArrayList<DiffAttribute> modifiedAttributes,Resource resource,
			ConfigService configService, AttributeList changedAttrList,Session session, AdminClient adminClient,ObjectName scope, 
			Resource allResources,Resource referenceResources,DeployInfo deployInfo)
		throws AttributeNotFoundException,ConnectorException,ConfigServiceException,DeployException{
		
		HashMap<String, String> resourceAttributeList = resource.getAttributeList(); 
		ResourceCreatorHelper resourceCreatorHelper = new ResourceCreatorHelper();
		if (configService.getAttribute(session, resourceWasObject, resourceAttributeName) !=null){

			String configAttributeValue = configService.getAttribute(session, resourceWasObject, resourceAttributeName).toString();
			String resourceAttributeValue =  resourceAttributeList.get(resourceAttributeName );
		
			if (((configAttributeValue==null) || (!configAttributeValue.equals(resourceAttributeValue))) && (resourceAttributeValue!=null)) {
				System.out.println(resourceAttributeName + " different resourceAttributeValue:" + resourceAttributeValue + " configAttributeValue:"+ configAttributeValue);
				
				resourceCreatorHelper.modifyAttribute(resource, resourceWasObject, resourceAttributeName, modifiedAttributes, changedAttrList,
						configService, session, adminClient, scope, allResources, referenceResources, deployInfo); 
			}
				
		}

	}

	
	/**
	 * 
	 * @param configObject
	 * @param atributeName
	 * @param incomingAttributeList
	 * @param resource
	 * @param configService
	 * @param session
	 * @throws ConfigServiceException
	 * @throws ConnectorException
	 */
	private void checkInCommingAttribute(ObjectName configObject, HashMap<String, String> incomingAttributeList ,
			Resource resource,ConfigService configService,Session session, DeployInfo deployInfo ,int count,String ObjectName)
	
		throws ConfigServiceException,ConnectorException,MalformedObjectNameException,AttributeNotFoundException,DeployException{
		
			
		AttributeList attrMetaInfoList = configService.getAttributesMetaInfo(ObjectName);
		Iterator attrMetaInfoListIterator = attrMetaInfoList.iterator();

		
		while (attrMetaInfoListIterator.hasNext()){
			
			Attribute configObjectAttributeMetaInfo = (Attribute)attrMetaInfoListIterator.next();
			String attributeName = configObjectAttributeMetaInfo.getName();
			// this is used when an already existing resource needs to be checked for incoming changes.
			// if we are checking an existing resource in the
			logger.trace(" >>> Start Checking attribute for existing resource " + attributeName );
			
			if (incomingAttributeList.get(attributeName) ==null){
				if ((resource.getAttributeList()==null) || (resource.getAttributeList().get(attributeName)==null)){
					if (!ResourceHelper.isOnIgnoreList(attributeName) ){
						//logger.trace(" >>> Start Checking attribute for existing resource " + attributeName );
						Object attributeValue = configService.getAttribute(session,configObject,attributeName);
						logger.trace(" Checking attribute " + attributeName + " with value " + attributeValue);
						wasConfigReaderHelper.addConfigAttributeToResourceAttributeMap(session, configService, attrMetaInfoList, attributeName,
								incomingAttributeList , resource, attributeValue, null,deployInfo, count);
	
						
					}
				}
			}
			logger.trace(" <<< Finished Checking attribute for existing resource " + attributeName );
		}
	}
	
	

}