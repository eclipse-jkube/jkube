echo ============================================
echo Deploying jshift-maven-plugin documentation
echo ============================================

cd doc && \
mvn -Phtml,pdf package && \
git clone -b gh-pages https://rhuss:${GITHUB_TOKEN}@github.com/jshiftio/jshift-kit.git gh-pages && \
git config --global user.email "travis@jshift.io" && \
git config --global user.name "Travis" && \
cp -rv target/generated-docs/* gh-pages/ && \
cd gh-pages && \
mv index.pdf jshift-kit.pdf && \
git add --ignore-errors * && \
git commit -m "generated documentation" && \
git push origin gh-pages && \
cd .. && \
rm -rf gh-pages target
