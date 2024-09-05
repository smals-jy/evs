#!/usr/bin/env bash
cd "$(dirname "$0")"
../system/vault-uploader.sh -rootDir="../exe/interaction/" -writeAsIs=false -exportAfterUpload=false -validateExportAfterUpload=false -generateGlobalMedicationScheme=true  -generateDailyMedicationScheme=false -generateSumehrOverview=true -generateGatewayMedicationScheme=true -startTransactonId=100 -shiftAction=no_tag_and_no_shift -hub=VITALINK -searchType=LOCAL -autoGenerateMSTransactionAuthor=true -generateDiaryNoteVisualization=true -generateVaccinationVisualization=true -generateChildPreventionVisualization=true -generatePopulationBasedScreeningVisualization=true -logCommunicationType=WITHOUT_SECURITY
