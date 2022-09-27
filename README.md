# Smart Tags Translation (Using Azure Cognitive Services for Translation)

## Introduction
This example AEM Cloud Services  project provides a custom workflow step for translating [AEM Assets Image Smart Tags](https://experienceleague.adobe.com/docs/experience-manager-learn/assets/metadata/image-smart-tags.html?lang=en) to a desired language using [Azure Congnitive Services for translation](https://azure.microsoft.com/en-us/products/cognitive-services/translator/#features).

## Pre-requisites
1. An AEM Cloud Services instance with custom code deployment permissions.
2. Azure Cognitive Services Subscription.

## How to use
1. Navigate to `Tools > Workflow > Models` and select `Asset Cloud Post-Processing` workflow for editing.

![image](https://user-images.githubusercontent.com/1191451/192443378-3c2a9761-ab9b-4b97-a6f4-d5f4032deb0d.png)

2. Search for "Translate Smart Tags Process" workflow step and add it before the `Workflow Completed` step.

![image](https://user-images.githubusercontent.com/1191451/192443771-60be69c0-4ad1-4daa-9c98-80b09e676c9f.png)

3. Configure the "Translate Smart Tags Process" workflow step added to the workflow by specifying the following:
  * `Target Language` - The language to which the smart tags must be translated to. For e.g. `de` for German, `fr` for French, `hi` for Hindi. 
  * `Subscription Key` - Subscription key for Azure Cognitive Services. You can get this from your Azure portal where you have setup the Azure Cognitive Services.
  * `Location` - Location for Azure Cognitive Services. You can get this from your Azure portal where you have setup the Azure Cognitive Services.

![image](https://user-images.githubusercontent.com/1191451/192443905-e2efd573-76da-44f9-ab4f-7381127be37f.png)

4. Click on the `Sync` button in the top-right corner to save the changes made to the workflow.

![image](https://user-images.githubusercontent.com/1191451/192444563-b1e1b75e-1e0e-480f-a5e0-02d1e8408191.png)

## Remember
1. **This is just an example implementation. Make sure to customize it as per your specific use case.** 
2. Translated Smart Tags remove the original `English` version of the smart tags.
3. This would increase the overall asset ingestion time as there's an additional call made to Azure Cognitive Service for each asset.
4. Using Azure Cognitive Services has additional cost implications.
