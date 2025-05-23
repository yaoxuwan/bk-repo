#!/usr/bin/env bash
# 用途：构建并推送docker镜像

# 安全模式
set -euo pipefail 

# 通用脚本框架变量
PROGRAM=$(basename "$0")
EXITCODE=0

ALL=1
GATEWAY=0
BACKEND=0
INIT=0
INIT_RBAC=0
BUILDPATH=
SERVICE=

cd $(dirname $0)
WORKING_DIR=$(pwd)
ROOT_DIR=${WORKING_DIR%/*/*/*}
BACKEND_DIR=$ROOT_DIR/src/backend
FRONTEND_DIR=$ROOT_DIR/src/frontend
GATEWAY_DIR=$ROOT_DIR/src/gateway

usage () {
    cat <<EOF
用法:
    $PROGRAM [OPTIONS]...

            [ --gateway             [可选] 打包gateway镜像 ]
            [ --backend             [可选] 打包backend镜像 ]
            [ --init                [可选] 打包init镜像 ]
            [ --init-rbac           [可选] 打包init-rbac镜像 ]
            [ -v, --version         [可选] 镜像版本tag, 默认latest ]
            [ -p, --push            [可选] 推送镜像到docker远程仓库，默认不推送 ]
            [ -r, --registry        [可选] docker仓库地址, 默认docker.io ]
            [ --username            [可选] docker仓库用户名 ]
            [ --password            [可选] docker仓库密码 ]
            [ --service             [可选] 需要编译的后端服务名,分割 ]
            [ -h, --help            [可选] 查看脚本帮助 ]
EOF
}

usage_and_exit () {
    usage
    exit "$1"
}

log () {
    echo "$@"
}

error () {
    echo "$@" 1>&2
    usage_and_exit 1
}

warning () {
    echo "$@" 1>&2
    EXITCODE=$((EXITCODE + 1))
}

core_service=("helm" "oci" "rpm" "npm" "maven" "pypi" "conan" "nuget" "generic" "cargo" "s3" "huggingface" "lfs"
             "git" "svn" "composer" "ddc")

build_backend () {
    log "构建${SERVICE}镜像..."
    if [[ $SERVICE == "fs-server" ]];then
        $BACKEND_DIR/gradlew -p $BACKEND_DIR :fs:boot-$SERVICE:build -P'devops.assemblyMode'=k8s -x test
    elif [[ " ${core_service[@]} " == *" $SERVICE "* ]];then
        $BACKEND_DIR/gradlew -p $BACKEND_DIR :core:$SERVICE:boot-$SERVICE:build -P'devops.assemblyMode'=k8s -x test
    else
        $BACKEND_DIR/gradlew -p $BACKEND_DIR :$SERVICE:boot-$SERVICE:build -P'devops.assemblyMode'=k8s -x test
    fi
    rm -rf $tmp_dir/*
    cp backend/startup.sh $tmp_dir/
    cp $BACKEND_DIR/release/boot-$SERVICE.jar $tmp_dir/app.jar
}

# 解析命令行参数，长短混合模式
(( $# == 0 )) && usage_and_exit 1
while (( $# > 0 )); do
    case "$1" in
        --gateway )
            ALL=0
            GATEWAY=1
            ;;
        --backend )
            ALL=0
            BACKEND=1
            ;;
        --buildpath )
            shift
            BUILDPATH=$1
            ;;
        --service )
            shift
            SERVICE=$1
            ;;
        --init )
            ALL=0
            INIT=1
            ;;
        --init-rbac )
            ALL=0
            INIT_RBAC=1
            ;;
        --help | -h | '-?' )
            usage_and_exit 0
            ;;
        -*)
            error "不可识别的参数: $1"
            ;;
        *)
            break
            ;;
    esac
    shift
done


# 创建临时目录
mkdir -p $WORKING_DIR/$BUILDPATH
tmp_dir=$WORKING_DIR/$BUILDPATH
echo $tmp_dir

# 编译frontend
if [[ $ALL -eq 1 || $GATEWAY -eq 1 ]] ; then
    log "编译frontend..."
    yarn --cwd $FRONTEND_DIR install
    yarn --cwd $FRONTEND_DIR run public

    # 打包gateway镜像
    log "构建gateway镜像..."
    rm -rf $tmp_dir/*
    cp -rf $FRONTEND_DIR/frontend $tmp_dir/
    cp -rf $GATEWAY_DIR $tmp_dir/gateway
    cp -rf gateway/startup.sh $tmp_dir/
    cp -rf $ROOT_DIR/scripts/render_tpl $tmp_dir/
    cp -rf $ROOT_DIR/support-files/templates $tmp_dir/
fi


# 构建backend镜像
if [[ $ALL -eq 1 || $BACKEND -eq 1 ]] ; then
        build_backend
fi


# 构建init镜像
if [[ $ALL -eq 1 || $INIT -eq 1 ]] ; then
    log "构建init镜像..."
    rm -rf $tmp_dir/*
    cp -rf init/init-mongodb.sh $tmp_dir/
    cp -rf $ROOT_DIR/support-files/sql/init-data.js $tmp_dir/
    cp -rf $ROOT_DIR/support-files/sql/init-data-tenant.js $tmp_dir/
    cp -rf $ROOT_DIR/support-files/sql/init-data-ext.js $tmp_dir/
fi

# 构建init-rbac镜像
if [[ $ALL -eq 1 || $INIT_RBAC -eq 1 ]] ; then
    log "构建init-rbac镜像..."
    rm -rf $tmp_dir/*
    mkdir -p $tmp_dir/support-files/bkiam
    cp -rf $ROOT_DIR/support-files/bkiam/* $tmp_dir/support-files/bkiam
fi

echo "BUILD SUCCESSFUL!"
