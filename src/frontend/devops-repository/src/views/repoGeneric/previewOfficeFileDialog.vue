<template>
    <bk-dialog
        class="preview-office-file-dialog"
        v-model="previewDialog.show"
        :width="dialogWidth"
        :show-footer="false"
        :title="($t('preview') + ' - ' + previewDialog.title)"
        @cancel="cancel"
    >
        <div>
            <vue-office-docx
                v-if="previewDocx"
                :src="dataSource"
                style="height: 100vh;"
            />
            <vue-office-excel
                v-if="previewExcel"
                :src="dataSource"
                :options="excelOptions"
                style="height: 100vh;"
            />
            <vue-office-pdf
                v-if="previewPdf"
                :src="dataSource"
            />
        </div>
    </bk-dialog>
</template>

<script>
    import VueOfficeDocx from '@vue-office/docx'
    import VueOfficeExcel from '@vue-office/excel'
    import VueOfficePdf from '@vue-office/pdf'
    import { customizePreviewOfficeFile } from '@repository/utils/previewOfficeFile'

    export default {
        name: 'PreviewOfficeFileDialog',
        components: { VueOfficeDocx, VueOfficeExcel, VueOfficePdf },
        data () {
            return {
                projectId: '',
                repoName: '',
                filePath: '',
                dataSource: '',
                previewDialog: {
                    title: '',
                    show: false,
                    isLoading: true
                },
                dialogWidth: window.innerWidth - 1000,
                excelOptions: {
                    xls: false, // 预览xlsx文件设为false；预览xls文件设为true
                    minColLength: 0, // excel最少渲染多少列，如果想实现xlsx文件内容有几列，就渲染几列，可以将此值设置为0.
                    minRowLength: 0, // excel最少渲染多少行，如果想实现根据xlsx实际函数渲染，可以将此值设置为0.
                    widthOffset: 10, // 如果渲染出来的结果感觉单元格宽度不够，可以在默认渲染的列表宽度上再加 Npx宽
                    heightOffset: 10, // 在默认渲染的列表高度上再加 Npx高
                    beforeTransformData: (workbookData) => {
                        return workbookData
                    }, // 底层通过exceljs获取excel文件内容，通过该钩子函数，可以对获取的excel文件内容进行修改，比如某个单元格的数据显示不正确，可以在此自行修改每个单元格的value值。
                    transformData: (workbookData) => {
                        return workbookData
                    } // 将获取到的excel数据进行处理之后且渲染到页面之前，可通过transformData对即将渲染的数据及样式进行修改，此时每个单元格的text值就是即将渲染到页面上的内容
                },
                previewDocx: false,
                previewExcel: false,
                previewPdf: false
            }
        },
        methods: {
            setData () {
                customizePreviewOfficeFile(this.projectId, this.repoName, this.filePath).then(res => {
                    if (this.filePath.endsWith('docx')) {
                        this.previewDocx = true
                    } else if (this.filePath.endsWith('xlsx')) {
                        this.previewExcel = true
                    } else if (this.filePath.endsWith('xls')) {
                        this.excelOptions.xls = true
                        this.previewExcel = true
                    } else {
                        this.previewPdf = true
                    }
                    this.dataSource = res.data
                    this.previewDialog.isLoading = false
                }).catch((e) => {
                    this.cancel()
                    const vm = window.repositoryVue
                    vm.$bkMessage({
                        theme: 'error',
                        message: e.message
                    })
                })
            },
            setDialogData (data) {
                this.previewDialog = {
                    ...data
                }
            },
            cancel () {
                this.previewDialog.show = false
                this.filePath = ''
                this.projectId = ''
                this.repoName = ''
                this.dataSource = ''
                this.previewDocx = false
                this.previewExcel = false
                this.previewPdf = false
            }
        }
    }
</script>

<style lang="scss" scoped>
@import '@vue-office/docx/lib/index.css';
@import '@vue-office/excel/lib/index.css';
    .preview-file-tips {
        margin-bottom: 10px;
        color: #707070;
    }
</style>
